package com.example.inventory.service;

import com.example.inventory.config.ProductCatalogClient;
import com.example.inventory.dto.ComboProductDto;
import com.example.inventory.dto.PackConversionDto;
import com.example.inventory.dto.ProductDto;
import com.example.inventory.enums.PackType;
import com.example.inventory.entity.Inventory;
import com.example.inventory.enums.InventoryState;
import com.example.inventory.exception.InventoryException;
import com.example.inventory.exception.InventoryException.InventoryErrorCode;
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
    public Inventory createInventory(Long productId, Integer quantity) {
        ProductDto product = productCatalogClient.getProduct(productId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                        "Product does not exist with ID: " + productId));

        if ("COMBO".equals(product.getType())) {
            createComboInventory(productId, quantity, PackType.valueOf(product.getPackType()));
        }

        return createSingleInventory(productId, quantity, product);
    }

    private void createComboInventory(Long comboId, Integer quantity, PackType comboPackType) {
        List<ComboProductDto> components = productCatalogClient.getComboProduct(comboId);
        if (components.isEmpty()) {
            throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                    "Combo definition not found for product ID: " + comboId);
        }

        for (ComboProductDto component : components) {
            Long componentId = component.getProductId();
            Integer componentRatio = component.getQuantity();
            Integer componentQuantity = quantity * componentRatio;

            ProductDto componentProduct = productCatalogClient.getProduct(componentId)
                    .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                            "Component product not found with ID: " + componentId));

            PackType componentPackType = PackType.valueOf(componentProduct.getPackType());
            int componentConvertedQuantity;

            if (comboPackType == componentPackType) {
                componentConvertedQuantity = componentQuantity;
            } else {
                PackConversionDto conversion = productCatalogClient
                        .getPackConversion(componentProduct.getSkuId(), new PackConversionDto.PackConversionQuery(comboPackType, componentPackType))
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PACK_CONVERSION_NOT_FOUND,
                                "Pack conversion not found for sku " + componentProduct.getSkuId()
                                        + " from " + comboPackType + " to " + componentPackType));

                componentConvertedQuantity = calculateConvertedQuantity(componentQuantity, conversion.getConversionFactor());
            }

            createInventory(componentId, componentConvertedQuantity);
        }
    }

    private Inventory createSingleInventory(Long productId, Integer quantity, ProductDto product) {
        PackType packType = PackType.valueOf(product.getPackType());
        Optional<Inventory> existingInventory = inventoryRepository.findByProductIdAndStateAndPackType(productId, InventoryState.AVAILABLE, packType);
        if (existingInventory.isPresent()) {
            Inventory inventory = existingInventory.get();
            inventory.setQuantity(inventory.getQuantity() + quantity);
            return inventoryRepository.save(inventory);
        }

        Inventory inventory = new Inventory(productId, packType, quantity, InventoryState.AVAILABLE);
        inventory.setId(null);
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void moveInventory(Long productId, InventoryState fromState, InventoryState toState, Integer quantityToMove) {
        ProductDto product = productCatalogClient.getProduct(productId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                        "Product does not exist with ID: " + productId));

        validateTransition(fromState, toState);

        if (quantityToMove <= 0) {
            throw new InventoryException(InventoryErrorCode.INSUFFICIENT_QUANTITY,
                    "Quantity to move must be positive");
        }

        // 1. Validation Phase - check all before executing any move
        checkSufficiencyRecursive(productId, fromState, quantityToMove, product);

        // 2. Execution Phase - move all atomically
        executeMoveRecursive(productId, fromState, toState, quantityToMove, product);
    }

    @Transactional
    public void convertInventory(String skuId, PackType fromPackType, PackType toPackType, Integer quantityToConvert) {
        if (fromPackType == toPackType) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    "From and to pack types must be different");
        }

        if (quantityToConvert <= 0) {
            throw new InventoryException(InventoryErrorCode.INSUFFICIENT_QUANTITY,
                    "Quantity to convert must be positive");
        }

        ProductDto fromProduct = productCatalogClient.getProductBySkuIdAndPackType(skuId, fromPackType)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                        "Product does not exist with sku: " + skuId + " and pack type: " + fromPackType));

        ProductDto toProduct = productCatalogClient.getProductBySkuIdAndPackType(skuId, toPackType)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                        "Product does not exist with sku: " + skuId + " and pack type: " + toPackType));

        if (!fromPackType.name().equals(fromProduct.getPackType())) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    "Product pack type " + fromProduct.getPackType() + " does not match from pack type " + fromPackType);
        }

        if (!toPackType.name().equals(toProduct.getPackType())) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    "Product pack type " + toProduct.getPackType() + " does not match to pack type " + toPackType);
        }

        PackConversionDto conversion = productCatalogClient
                .getPackConversion(skuId, new PackConversionDto.PackConversionQuery(fromPackType, toPackType))
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.PACK_CONVERSION_NOT_FOUND,
                        "Pack conversion not found for sku " + skuId + " from " + fromPackType + " to " + toPackType));

        int convertedQuantity = calculateConvertedQuantity(quantityToConvert, conversion.getConversionFactor());

        checkConversionSufficiencyRecursive(fromProduct.getId(), quantityToConvert, fromProduct, fromPackType);
        executeConversionRecursive(fromProduct.getId(), toProduct.getId(), fromPackType, toPackType,
                quantityToConvert, convertedQuantity, fromProduct);
    }

    private void checkSufficiencyRecursive(Long productId, InventoryState state, Integer quantity, ProductDto product) {
        PackType packType = PackType.valueOf(product.getPackType());
        Inventory inventory = inventoryRepository.findByProductIdAndStateAndPackType(productId, state, packType)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        "No inventory found for product " + productId + " in state " + state + " with pack type " + packType));

        if (inventory.getQuantity() < quantity) {
            throw new InventoryException(InventoryErrorCode.INSUFFICIENT_QUANTITY,
                    "Insufficient quantity for product " + productId + " in " + state
                            + " state. Available: " + inventory.getQuantity() + ", Requested: " + quantity);
        }

        if ("COMBO".equals(product.getType())) {
            List<ComboProductDto> components = productCatalogClient.getComboProduct(productId);
            if (components.isEmpty()) {
                throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                        "Combo definition not found for product ID: " + productId);
            }

            for (ComboProductDto component : components) {
                Long componentId = component.getProductId();
                Integer componentRatio = component.getQuantity();
                Integer componentQuantity = quantity * componentRatio;

                ProductDto componentProduct = productCatalogClient.getProduct(componentId)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                "Component product not found with ID: " + componentId));

                checkSufficiencyRecursive(componentId, state, componentQuantity, componentProduct);
            }
        }
    }

    private void executeMoveRecursive(Long productId, InventoryState fromState, InventoryState toState, Integer quantity, ProductDto product) {
        PackType packType = PackType.valueOf(product.getPackType());
        moveSingleInventory(productId, fromState, toState, quantity, packType);

        if ("COMBO".equals(product.getType())) {
            List<ComboProductDto> components = productCatalogClient.getComboProduct(productId);
            if (components.isEmpty()) {
                throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                        "Combo definition not found for product ID: " + productId);
            }

            for (ComboProductDto component : components) {
                Long componentId = component.getProductId();
                Integer componentRatio = component.getQuantity();
                Integer componentQuantity = quantity * componentRatio;

                ProductDto componentProduct = productCatalogClient.getProduct(componentId)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                "Component product not found with ID: " + componentId));

                executeMoveRecursive(componentId, fromState, toState, componentQuantity, componentProduct);
            }
        }
    }

    private void moveSingleInventory(Long productId, InventoryState fromState, InventoryState toState, Integer quantityToMove, PackType packType) {
        Inventory fromInventory = inventoryRepository.findByProductIdAndStateAndPackType(productId, fromState, packType)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        "No inventory found for product " + productId + " in state " + fromState + " with pack type " + packType));

        fromInventory.setQuantity(fromInventory.getQuantity() - quantityToMove);
        inventoryRepository.save(fromInventory);

        Optional<Inventory> toInventoryOpt = inventoryRepository.findByProductIdAndStateAndPackType(productId, toState, packType);
        if (toInventoryOpt.isPresent()) {
            Inventory toInventory = toInventoryOpt.get();
            toInventory.setQuantity(toInventory.getQuantity() + quantityToMove);
            inventoryRepository.save(toInventory);
        } else {
            Inventory toInventory = new Inventory(productId, packType, quantityToMove, toState);
            toInventory.setId(null);
            inventoryRepository.save(toInventory);
        }
    }

    private void validateTransition(InventoryState fromState, InventoryState toState) {
        if (fromState == InventoryState.COMPLETE) {
            throw new InventoryException(InventoryErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot move inventory from COMPLETE state — it is a terminal state");
        }

        boolean valid = (fromState == InventoryState.AVAILABLE && toState == InventoryState.ALLOCATED)
                || (fromState == InventoryState.ALLOCATED && toState == InventoryState.COMPLETE)
                || (fromState == InventoryState.ALLOCATED && toState == InventoryState.AVAILABLE);

        if (!valid) {
            throw new InventoryException(InventoryErrorCode.INVALID_STATE_TRANSITION,
                    "Invalid state transition from " + fromState + " to " + toState
                            + ". Allowed: AVAILABLE→ALLOCATED, ALLOCATED→COMPLETE, ALLOCATED→AVAILABLE");
        }
    }

    private int calculateConvertedQuantity(int quantityToConvert, int conversionFactor) {
        if (conversionFactor <= 0) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    "Conversion factor must be positive");
        }
        long converted = (long) quantityToConvert * conversionFactor;
        if (converted > Integer.MAX_VALUE) {
            throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                    "Converted quantity exceeds supported range");
        }
        return (int) converted;
    }

    private void checkConversionSufficiencyRecursive(Long productId, Integer quantity, ProductDto product, PackType fromPackType) {
        PackType packType = PackType.valueOf(product.getPackType());
        Inventory inventory = inventoryRepository.findByProductIdAndStateAndPackType(productId, InventoryState.AVAILABLE, packType)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        "No inventory found for product " + productId + " in AVAILABLE state with pack type " + packType));

        if (inventory.getQuantity() < quantity) {
            throw new InventoryException(InventoryErrorCode.INSUFFICIENT_QUANTITY,
                    "Insufficient quantity for product " + productId + " in AVAILABLE state. Available: "
                            + inventory.getQuantity() + ", Requested: " + quantity);
        }

        if ("COMBO".equals(product.getType())) {
            List<ComboProductDto> components = productCatalogClient.getComboProduct(productId);
            if (components.isEmpty()) {
                throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                        "Combo definition not found for product ID: " + productId);
            }

            for (ComboProductDto component : components) {
                Long componentId = component.getProductId();
                Integer componentRatio = component.getQuantity();
                Integer componentQuantity = quantity * componentRatio;

                ProductDto componentProduct = productCatalogClient.getProduct(componentId)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                "Component product not found with ID: " + componentId));

                if (!fromPackType.name().equals(componentProduct.getPackType())) {
                    throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                            "Component product pack type " + componentProduct.getPackType()
                                    + " does not match from pack type " + fromPackType);
                }

                checkConversionSufficiencyRecursive(componentId, componentQuantity, componentProduct, fromPackType);
            }
        }
    }

    private void executeConversionRecursive(Long fromProductId, Long toProductId, PackType fromPackType, PackType toPackType,
                                            Integer quantityToConvert, Integer convertedQuantity, ProductDto fromProduct) {
        applyConversion(fromProductId, toProductId, fromPackType, toPackType, quantityToConvert, convertedQuantity);

        if ("COMBO".equals(fromProduct.getType())) {
            List<ComboProductDto> components = productCatalogClient.getComboProduct(fromProductId);
            if (components.isEmpty()) {
                throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                        "Combo definition not found for product ID: " + fromProductId);
            }

            for (ComboProductDto component : components) {
                Long componentId = component.getProductId();
                Integer componentRatio = component.getQuantity();
                Integer componentQuantity = quantityToConvert * componentRatio;

                ProductDto componentFromProduct = productCatalogClient.getProduct(componentId)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                "Component product not found with ID: " + componentId));

                PackConversionDto componentConversion = productCatalogClient
                        .getPackConversion(componentFromProduct.getSkuId(), new PackConversionDto.PackConversionQuery(fromPackType, toPackType))
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PACK_CONVERSION_NOT_FOUND,
                                "Pack conversion not found for sku " + componentFromProduct.getSkuId() + " from " + fromPackType + " to " + toPackType));

                int componentConvertedQuantity = calculateConvertedQuantity(componentQuantity, componentConversion.getConversionFactor());

                if (!fromPackType.name().equals(componentFromProduct.getPackType())) {
                    throw new InventoryException(InventoryErrorCode.INVALID_PACK_CONVERSION,
                            "Component product pack type " + componentFromProduct.getPackType()
                                    + " does not match from pack type " + fromPackType);
                }

                ProductDto componentToProduct = productCatalogClient.getProductBySkuIdAndPackType(componentFromProduct.getSkuId(), toPackType)
                        .orElseThrow(() -> new InventoryException(InventoryErrorCode.PRODUCT_NOT_FOUND,
                                "Component product not found with sku: " + componentFromProduct.getSkuId() + " and pack type: " + toPackType));

                executeConversionRecursive(componentId, componentToProduct.getId(), fromPackType, toPackType,
                        componentQuantity, componentConvertedQuantity, componentFromProduct);
            }
        }
    }

    private void applyConversion(Long fromProductId, Long toProductId, PackType fromPackType, PackType toPackType,
                                 Integer quantityToConvert, Integer convertedQuantity) {
        Inventory fromInventory = inventoryRepository.findByProductIdAndStateAndPackType(fromProductId, InventoryState.AVAILABLE, fromPackType)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        "No inventory found for product " + fromProductId + " in AVAILABLE state with pack type " + fromPackType));

        fromInventory.setQuantity(fromInventory.getQuantity() - quantityToConvert);
        inventoryRepository.save(fromInventory);

        Inventory toInventory = inventoryRepository.findByProductIdAndStateAndPackType(toProductId, InventoryState.AVAILABLE, toPackType)
                .orElse(null);

        if (toInventory != null) {
            toInventory.setQuantity(toInventory.getQuantity() + convertedQuantity);
            inventoryRepository.save(toInventory);
        } else {
            Inventory created = new Inventory(toProductId, toPackType, convertedQuantity, InventoryState.AVAILABLE);
            created.setId(null);
            inventoryRepository.save(created);
        }
    }

    public List<Inventory> getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }
}




