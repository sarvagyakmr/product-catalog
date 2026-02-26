package com.example.inventory.service;

import com.example.inventory.config.ProductCatalogClient;
import com.example.inventory.dto.ComboProductDto;
import com.example.inventory.dto.ProductDto;
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
            createComboInventory(productId, quantity);
        }

        return createSingleInventory(productId, quantity);
    }

    private void createComboInventory(Long comboId, Integer quantity) {
        List<ComboProductDto> components = productCatalogClient.getComboProduct(comboId);
        if (components.isEmpty()) {
            throw new InventoryException(InventoryErrorCode.COMBO_DEFINITION_NOT_FOUND,
                    "Combo definition not found for product ID: " + comboId);
        }

        for (ComboProductDto component : components) {
            Long componentId = component.getProductId();
            Integer componentRatio = component.getQuantity();
            Integer componentQuantity = quantity * componentRatio;
            createInventory(componentId, componentQuantity);
        }
    }

    private Inventory createSingleInventory(Long productId, Integer quantity) {
        Optional<Inventory> existingInventory = inventoryRepository.findByProductIdAndState(productId, InventoryState.AVAILABLE);
        if (existingInventory.isPresent()) {
            Inventory inventory = existingInventory.get();
            inventory.setQuantity(inventory.getQuantity() + quantity);
            return inventoryRepository.save(inventory);
        }

        Inventory inventory = new Inventory(productId, quantity, InventoryState.AVAILABLE);
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

    private void checkSufficiencyRecursive(Long productId, InventoryState state, Integer quantity, ProductDto product) {
        Inventory inventory = inventoryRepository.findByProductIdAndState(productId, state)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        "No inventory found for product " + productId + " in state " + state));

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
        moveSingleInventory(productId, fromState, toState, quantity);

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

    private void moveSingleInventory(Long productId, InventoryState fromState, InventoryState toState, Integer quantityToMove) {
        Inventory fromInventory = inventoryRepository.findByProductIdAndState(productId, fromState)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND,
                        "No inventory found for product " + productId + " in state " + fromState));

        fromInventory.setQuantity(fromInventory.getQuantity() - quantityToMove);
        inventoryRepository.save(fromInventory);

        Optional<Inventory> toInventoryOpt = inventoryRepository.findByProductIdAndState(productId, toState);
        if (toInventoryOpt.isPresent()) {
            Inventory toInventory = toInventoryOpt.get();
            toInventory.setQuantity(toInventory.getQuantity() + quantityToMove);
            inventoryRepository.save(toInventory);
        } else {
            Inventory toInventory = new Inventory(productId, quantityToMove, toState);
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
}




