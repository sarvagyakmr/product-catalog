package com.example.commons.client;

import com.example.commons.enums.InventoryState;
import com.example.commons.enums.PackType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/**
 * Client for communicating with the Inventory Service.
 * Provides methods for inventory CRUD and state transitions.
 */
@Component
public class InventoryServiceClient {

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryServiceClient(@Value("${inventory.service.url:http://localhost:8081}") String inventoryServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    // ===== Read Operations =====

    public Integer getAvailableQuantity(Long productId, PackType packType, Long warehouseId) {
        String url = inventoryServiceUrl + "/api/inventory/product/" + productId;
        if (warehouseId != null) {
            url += "?warehouseId=" + warehouseId;
        }
        try {
            InventoryDto[] inventoryArray = restTemplate.getForObject(url, InventoryDto[].class);
            if (inventoryArray != null) {
                return Arrays.stream(inventoryArray)
                    .filter(inv -> inv.getState() == InventoryState.AVAILABLE && inv.getPackType() == packType)
                    .map(InventoryDto::getQuantity)
                    .reduce(0, Integer::sum);
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    // ===== Write Operations =====

    public void createInventory(Long productId, Integer quantity, Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        CreateInventoryRequest request = new CreateInventoryRequest(productId, quantity, warehouseId);
        restTemplate.postForObject(inventoryServiceUrl + "/api/inventory", request, Object.class);
    }

    public void updateInventory(Long productId, Integer quantity, Long warehouseId) {
        createInventory(productId, quantity, warehouseId);
    }

    public void updateReceivedInventory(Long productId, Integer quantity, Long warehouseId) {
        createInventory(productId, quantity, warehouseId);
    }

    public void moveInventory(Long productId, InventoryState fromState, InventoryState toState, Integer quantity, Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        MoveInventoryRequest request = new MoveInventoryRequest(productId, fromState, toState, quantity, warehouseId);
        restTemplate.postForObject(inventoryServiceUrl + "/api/inventory/move", request, Object.class);
    }

    public void allocateInventory(Long productId, Integer quantity, Long warehouseId) {
        moveInventory(productId, InventoryState.AVAILABLE, InventoryState.ALLOCATED, quantity, warehouseId);
    }

    // ===== Request DTOs =====

    public static class CreateInventoryRequest {
        private Long productId;
        private Integer quantity;
        private Long warehouseId;

        public CreateInventoryRequest() {}

        public CreateInventoryRequest(Long productId, Integer quantity, Long warehouseId) {
            this.productId = productId;
            this.quantity = quantity;
            this.warehouseId = warehouseId;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    }

    public static class MoveInventoryRequest {
        private Long productId;
        private InventoryState fromState;
        private InventoryState toState;
        private Integer quantity;
        private Long warehouseId;

        public MoveInventoryRequest() {}

        public MoveInventoryRequest(Long productId, InventoryState fromState, InventoryState toState, Integer quantity, Long warehouseId) {
            this.productId = productId;
            this.fromState = fromState;
            this.toState = toState;
            this.quantity = quantity;
            this.warehouseId = warehouseId;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public InventoryState getFromState() { return fromState; }
        public void setFromState(InventoryState fromState) { this.fromState = fromState; }
        public InventoryState getToState() { return toState; }
        public void setToState(InventoryState toState) { this.toState = toState; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    }

    // ===== Response DTO =====

    public static class InventoryDto {
        private Long id;
        private Long productId;
        private PackType packType;
        private Integer quantity;
        private InventoryState state;
        private Long warehouseId;

        public InventoryDto() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public PackType getPackType() { return packType; }
        public void setPackType(PackType packType) { this.packType = packType; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public InventoryState getState() { return state; }
        public void setState(InventoryState state) { this.state = state; }
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    }
}
