package com.example.inventory.service;

import com.example.commons.client.ProductCatalogClient;
import com.example.commons.constants.ErrorMessages;
import com.example.commons.dto.ComboProductDto;
import com.example.commons.dto.PackConversionDto;
import com.example.commons.dto.ProductDto;
import com.example.commons.enums.InventoryState;
import com.example.commons.enums.PackType;
import com.example.commons.exception.InventoryException;
import com.example.commons.exception.InventoryException.InventoryErrorCode;
import com.example.inventory.entity.Inventory;
import com.example.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductCatalogClient productCatalogClient;

    public InventoryService(InventoryRepository inventoryRepository, ProductCatalogClient productCatalogClient) {
        this.inventoryRepository = inventoryRepository;
        this.productCatalogClient = productCatalogClient;
    }

    @Transactional
    public Inventory createInventory(Long productId, Integer quantity, Long warehouseId) {
        if (warehouseId == null) {
            throw new InventoryException(InventoryErrorCode.INVALID_REQUEST, "Warehouse ID is required");
        }
        ProductDto product = productCatalogClient.getProduct(productId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                        String.format(ErrorMessages.PRODUCT_NOT_FOUND_WITH_ID, productId)));

        if ("COMBO".equals(product.getType())) {
            createComboInventory(productId, quantity, PackType.valueOf(product.getPackType()), warehouseId);
        }

        return createSingleInventory(productId, quantity, product, warehouseId);
    }

    private void createComboInventory(Long comboId, Integer quantity, PackType comboPackType, Long warehouseId) {
        List<ComboProductDto> components = productCatalogClient.getComboProduct(comboId);
        if (components.isEmpty()) {
            throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                    String.format(ErrorMessages.COMBO_DEFINITION_NOT_FOUND, comboId));
        }

        for (ComboProductDto component : components) {
            Long componentId = component.getProductId();
            Integer componentRatio = component.getQuantity();
            Integer componentQuantity = quantity * componentRatio;

            ProductDto componentProduct = productCatalogClient.getProduct(componentId)
                    .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                            String.format(ErrorMessages.COMPONENT_PRODUCT_NOT_FOUND_WITH_ID, componentId)));

            PackType componentPackType = PackType.valueOf(componentProduct.getPackType());
            int componentConvertedQuantity;

            if (comboPackType == componentPackType) {
                componentConvertedQuantity = componentQuantity;
            } else {
                PackConversionDto conversion = productCatalogClient
                        .getPackConversion(componentProduct.getSkuId(), new PackConversionDto.PackConversionQuery(comboPackType, componentPackType))
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PACK_CONVERSION_NOT_FOUND,
                                String.format(ErrorMessages.PACK_CONVERSION_NOT_FOUND, componentProduct.getSkuId(), comboPackType, componentPackType)));

                componentConvertedQuantity = calculateConvertedQuantity(componentQuantity, conversion.getConversionFactor());
            }

            createInventory(componentId, componentConvertedQuantity, warehouseId);
        }
    }

    private Inventory createSingleInventory(Long productId, Integer quantity, ProductDto product, Long warehouseId) {
        PackType packType = PackType.valueOf(product.getPackType());
        Optional<Inventory> existingInventory = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(productId, InventoryState.RECEIVED, packType, warehouseId);
        if (existingInventory.isPresent()) {
            Inventory inventory = existingInventory.get();
            inventory.setQuantity(inventory.getQuantity() + quantity);
            return inventoryRepository.save(inventory);
        }

        Inventory inventory = new Inventory(productId, packType, quantity, InventoryState.RECEIVED, warehouseId);
        inventory.setId(null);
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void moveInventory(Long productId, InventoryState fromState, InventoryState toState, Integer quantityToMove, Long warehouseId) {
        if (warehouseId == null) {
            throw new InventoryException(InventoryErrorCode.INVALID_REQUEST, "Warehouse ID is required");
        }
        ProductDto product = productCatalogClient.getProduct(productId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                        String.format(ErrorMessages.PRODUCT_NOT_FOUND_WITH_ID, productId)));

        validateTransition(fromState, toState);

        if (quantityToMove <= 0) {
            throw new InventoryException(InventoryErrorCode.INSUFFICIENT_QUANTITY,
                    ErrorMessages.QUANTITY_MUST_BE_POSITIVE);
        }

        // 1. Validation Phase - check all before executing any move
        checkSufficiencyRecursive(productId, fromState, quantityToMove, product, warehouseId);

        // 2. Execution Phase - move all atomically
        executeMoveRecursive(productId, fromState, toState, quantityToMove, product, warehouseId);
    }

    @Transactional
    public void convertInventory(String skuId, PackType fromPackType, PackType toPackType, Integer quantityToConvert, Long warehouseId) {
        if (warehouseId == null) {
            throw new InventoryException(InventoryErrorCode.INVALID_REQUEST, "Warehouse ID is required");
        }
        if (fromPackType == toPackType) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    ErrorMessages.PACK_CONVERSION_SAME_TYPE);
        }

        if (quantityToConvert <= 0) {
            throw new InventoryException(InventoryErrorCode.INSUFFICIENT_QUANTITY,
                    ErrorMessages.QUANTITY_TO_CONVERT_MUST_BE_POSITIVE);
        }

        ProductDto fromProduct = productCatalogClient.getProductBySkuIdAndPackType(skuId, fromPackType)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                        String.format(ErrorMessages.PRODUCT_NOT_FOUND_WITH_SKU_AND_PACK_TYPE, skuId, fromPackType)));

        ProductDto toProduct = productCatalogClient.getProductBySkuIdAndPackType(skuId, toPackType)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                        String.format(ErrorMessages.PRODUCT_NOT_FOUND_WITH_SKU_AND_PACK_TYPE, skuId, toPackType)));

        if (!fromPackType.name().equals(fromProduct.getPackType())) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    String.format(ErrorMessages.PACK_TYPE_MISMATCH_FROM, fromProduct.getPackType(), fromPackType));
        }

        if (!toPackType.name().equals(toProduct.getPackType())) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    String.format(ErrorMessages.PACK_TYPE_MISMATCH_TO, toProduct.getPackType(), toPackType));
        }

        PackConversionDto conversion = productCatalogClient
                .getPackConversion(skuId, new PackConversionDto.PackConversionQuery(fromPackType, toPackType))
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PACK_CONVERSION_NOT_FOUND,
                        String.format(ErrorMessages.PACK_CONVERSION_NOT_FOUND, skuId, fromPackType, toPackType)));

        int convertedQuantity = calculateConvertedQuantity(quantityToConvert, conversion.getConversionFactor());

        checkConversionSufficiencyRecursive(fromProduct.getId(), quantityToConvert, fromProduct, fromPackType, warehouseId);
        executeConversionRecursive(fromProduct.getId(), toProduct.getId(), fromPackType, toPackType,
                quantityToConvert, convertedQuantity, fromProduct, warehouseId);
    }

    private void checkSufficiencyRecursive(Long productId, InventoryState state, Integer quantity, ProductDto product, Long warehouseId) {
        PackType packType = PackType.valueOf(product.getPackType());
        Inventory inventory = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(productId, state, packType, warehouseId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        String.format(ErrorMessages.INVENTORY_NOT_FOUND_WITH_STATE, productId, state, packType)));

        if (inventory.getQuantity() < quantity) {
            throw new InventoryException(InventoryErrorCode.INSUFFICIENT_QUANTITY,
                    String.format(ErrorMessages.INSUFFICIENT_QUANTITY_WITH_STATE, productId, state, inventory.getQuantity(), quantity));
        }

        if ("COMBO".equals(product.getType())) {
            List<ComboProductDto> components = productCatalogClient.getComboProduct(productId);
            if (components.isEmpty()) {
                throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                        String.format(ErrorMessages.COMBO_DEFINITION_NOT_FOUND, productId));
            }

            for (ComboProductDto component : components) {
                Long componentId = component.getProductId();
                Integer componentRatio = component.getQuantity();
                Integer componentQuantity = quantity * componentRatio;

                ProductDto componentProduct = productCatalogClient.getProduct(componentId)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                String.format(ErrorMessages.COMPONENT_PRODUCT_NOT_FOUND_WITH_ID, componentId)));

                checkSufficiencyRecursive(componentId, state, componentQuantity, componentProduct, warehouseId);
            }
        }
    }

    private void executeMoveRecursive(Long productId, InventoryState fromState, InventoryState toState, Integer quantity, ProductDto product, Long warehouseId) {
        PackType packType = PackType.valueOf(product.getPackType());
        moveSingleInventory(productId, fromState, toState, quantity, packType, warehouseId);

        if ("COMBO".equals(product.getType())) {
            List<ComboProductDto> components = productCatalogClient.getComboProduct(productId);
            if (components.isEmpty()) {
                throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                        String.format(ErrorMessages.COMBO_DEFINITION_NOT_FOUND, productId));
            }

            for (ComboProductDto component : components) {
                Long componentId = component.getProductId();
                Integer componentRatio = component.getQuantity();
                Integer componentQuantity = quantity * componentRatio;

                ProductDto componentProduct = productCatalogClient.getProduct(componentId)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                String.format(ErrorMessages.COMPONENT_PRODUCT_NOT_FOUND_WITH_ID, componentId)));

                executeMoveRecursive(componentId, fromState, toState, componentQuantity, componentProduct, warehouseId);
            }
        }
    }

    private void moveSingleInventory(Long productId, InventoryState fromState, InventoryState toState, Integer quantityToMove, PackType packType, Long warehouseId) {
        Inventory fromInventory = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(productId, fromState, packType, warehouseId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        String.format(ErrorMessages.INVENTORY_NOT_FOUND_WITH_STATE, productId, fromState, packType)));

        fromInventory.setQuantity(fromInventory.getQuantity() - quantityToMove);
        inventoryRepository.save(fromInventory);

        Optional<Inventory> toInventoryOpt = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(productId, toState, packType, warehouseId);
        if (toInventoryOpt.isPresent()) {
            Inventory toInventory = toInventoryOpt.get();
            toInventory.setQuantity(toInventory.getQuantity() + quantityToMove);
            inventoryRepository.save(toInventory);
        } else {
            Inventory toInventory = new Inventory(productId, packType, quantityToMove, toState, warehouseId);
            toInventory.setId(null);
            inventoryRepository.save(toInventory);
        }
    }

    private void validateTransition(InventoryState fromState, InventoryState toState) {
        if (fromState == InventoryState.COMPLETE) {
            throw new InventoryException(InventoryErrorCode.INVALID_STATE_TRANSITION,
                    ErrorMessages.COMPLETE_STATE_TERMINAL);
        }

        boolean valid = (fromState == InventoryState.AVAILABLE && toState == InventoryState.ALLOCATED)
                || (fromState == InventoryState.ALLOCATED && toState == InventoryState.COMPLETE)
                || (fromState == InventoryState.ALLOCATED && toState == InventoryState.AVAILABLE)
                || (fromState == InventoryState.RECEIVED && toState == InventoryState.AVAILABLE);

        if (!valid) {
            throw new InventoryException(InventoryErrorCode.INVALID_STATE_TRANSITION,
                    String.format(ErrorMessages.INVALID_STATE_TRANSITION, fromState, toState));
        }
    }

    private int calculateConvertedQuantity(int quantityToConvert, int conversionFactor) {
        if (conversionFactor <= 0) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    ErrorMessages.CONVERSION_FACTOR_POSITIVE);
        }
        long converted = (long) quantityToConvert * conversionFactor;
        if (converted > Integer.MAX_VALUE) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    ErrorMessages.CONVERTED_QUANTITY_EXCEEDS_RANGE);
        }
        return (int) converted;
    }

    private void checkConversionSufficiencyRecursive(Long productId, Integer quantity, ProductDto product, PackType fromPackType, Long warehouseId) {
        PackType packType = PackType.valueOf(product.getPackType());
        Inventory inventory = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(productId, InventoryState.AVAILABLE, packType, warehouseId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        String.format(ErrorMessages.INVENTORY_NOT_FOUND_AVAILABLE, productId, packType)));

        if (inventory.getQuantity() < quantity) {
            throw new InventoryException(InventoryErrorCode.INSUFFICIENT_QUANTITY,
                    String.format(ErrorMessages.INSUFFICIENT_QUANTITY_AVAILABLE, productId, inventory.getQuantity(), quantity));
        }

        if ("COMBO".equals(product.getType())) {
            List<ComboProductDto> components = productCatalogClient.getComboProduct(productId);
            if (components.isEmpty()) {
                throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                        String.format(ErrorMessages.COMBO_DEFINITION_NOT_FOUND, productId));
            }

            for (ComboProductDto component : components) {
                Long componentId = component.getProductId();
                Integer componentRatio = component.getQuantity();
                Integer componentQuantity = quantity * componentRatio;

                ProductDto componentProduct = productCatalogClient.getProduct(componentId)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                String.format(ErrorMessages.COMPONENT_PRODUCT_NOT_FOUND_WITH_ID, componentId)));

                if (!fromPackType.name().equals(componentProduct.getPackType())) {
                    throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                            String.format(ErrorMessages.COMPONENT_PACK_TYPE_MISMATCH, componentProduct.getPackType(), fromPackType));
                }

                checkConversionSufficiencyRecursive(componentId, componentQuantity, componentProduct, fromPackType, warehouseId);
            }
        }
    }

    private void executeConversionRecursive(Long fromProductId, Long toProductId, PackType fromPackType, PackType toPackType,
                                            Integer quantityToConvert, Integer convertedQuantity, ProductDto fromProduct, Long warehouseId) {
        applyConversion(fromProductId, toProductId, fromPackType, toPackType, quantityToConvert, convertedQuantity, warehouseId);

        if ("COMBO".equals(fromProduct.getType())) {
            List<ComboProductDto> components = productCatalogClient.getComboProduct(fromProductId);
            if (components.isEmpty()) {
                throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                        String.format(ErrorMessages.COMBO_DEFINITION_NOT_FOUND, fromProductId));
            }

            for (ComboProductDto component : components) {
                Long componentId = component.getProductId();
                Integer componentRatio = component.getQuantity();
                Integer componentQuantity = quantityToConvert * componentRatio;

                ProductDto componentFromProduct = productCatalogClient.getProduct(componentId)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                String.format(ErrorMessages.COMPONENT_PRODUCT_NOT_FOUND_WITH_ID, componentId)));

                PackConversionDto componentConversion = productCatalogClient
                        .getPackConversion(componentFromProduct.getSkuId(), new PackConversionDto.PackConversionQuery(fromPackType, toPackType))
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PACK_CONVERSION_NOT_FOUND,
                                String.format(ErrorMessages.PACK_CONVERSION_NOT_FOUND, componentFromProduct.getSkuId(), fromPackType, toPackType)));

                int componentConvertedQuantity = calculateConvertedQuantity(componentQuantity, componentConversion.getConversionFactor());

                if (!fromPackType.name().equals(componentFromProduct.getPackType())) {
                    throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                            String.format(ErrorMessages.COMPONENT_PACK_TYPE_MISMATCH, componentFromProduct.getPackType(), fromPackType));
                }

                ProductDto componentToProduct = productCatalogClient.getProductBySkuIdAndPackType(componentFromProduct.getSkuId(), toPackType)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                String.format(ErrorMessages.PRODUCT_NOT_FOUND_WITH_SKU_AND_PACK_TYPE, componentFromProduct.getSkuId(), toPackType)));

                executeConversionRecursive(componentId, componentToProduct.getId(), fromPackType, toPackType,
                        componentQuantity, componentConvertedQuantity, componentFromProduct, warehouseId);
            }
        }
    }

    private void applyConversion(Long fromProductId, Long toProductId, PackType fromPackType, PackType toPackType,
                                 Integer quantityToConvert, Integer convertedQuantity, Long warehouseId) {
        Inventory fromInventory = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(fromProductId, InventoryState.AVAILABLE, fromPackType, warehouseId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        String.format(ErrorMessages.INVENTORY_NOT_FOUND_AVAILABLE, fromProductId, fromPackType)));

        fromInventory.setQuantity(fromInventory.getQuantity() - quantityToConvert);
        inventoryRepository.save(fromInventory);

        Optional<Inventory> toInventoryOpt = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(toProductId, InventoryState.AVAILABLE, toPackType, warehouseId);

        if (toInventoryOpt.isPresent()) {
            Inventory toInventory = toInventoryOpt.get();
            toInventory.setQuantity(toInventory.getQuantity() + convertedQuantity);
            inventoryRepository.save(toInventory);
        } else {
            Inventory created = new Inventory(toProductId, toPackType, convertedQuantity, InventoryState.AVAILABLE, warehouseId);
            created.setId(null);
            inventoryRepository.save(created);
        }
    }

    public List<Inventory> getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    public List<Inventory> getInventoryByProductIdAndWarehouseId(Long productId, Long warehouseId) {
        if (warehouseId == null) {
            throw new InventoryException(InventoryErrorCode.INVALID_REQUEST, "Warehouse ID is required");
        }
        return inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId);
    }

    public Integer getAvailableQuantity(Long productId, PackType packType, Long warehouseId) {
        if (warehouseId == null) {
            throw new InventoryException(InventoryErrorCode.INVALID_REQUEST, "Warehouse ID is required");
        }
        Optional<Inventory> inventory = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(productId, InventoryState.AVAILABLE, packType, warehouseId);
        return inventory.map(Inventory::getQuantity).orElse(0);
    }

    public Integer getAllocatedQuantity(Long productId, PackType packType, Long warehouseId) {
        if (warehouseId == null) {
            throw new InventoryException(InventoryErrorCode.INVALID_REQUEST, "Warehouse ID is required");
        }
        Optional<Inventory> inventory = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(productId, InventoryState.ALLOCATED, packType, warehouseId);
        return inventory.map(Inventory::getQuantity).orElse(0);
    }
}




