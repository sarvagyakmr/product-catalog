package com.example.warehousemanagement.client;

import com.example.commons.enums.InventoryState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryServiceClient {

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryServiceClient(@Value("${inventory.service.url:http://localhost:8081}") String inventoryServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    public void updateInventory(Long productId, Integer quantity, Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        CreateInventoryRequest request = new CreateInventoryRequest(productId, quantity, warehouseId);
        restTemplate.postForObject(inventoryServiceUrl + "/api/inventory", request, Object.class);
    }

    public void moveInventory(Long productId, InventoryState fromState, InventoryState toState, Integer quantity, Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        MoveInventoryRequest request = new MoveInventoryRequest(productId, fromState, toState, quantity, warehouseId);
        restTemplate.postForObject(inventoryServiceUrl + "/api/inventory/move", request, Object.class);
    }

    private static class CreateInventoryRequest {
        private Long productId;
        private Integer quantity;
        private Long warehouseId;

        public CreateInventoryRequest() {
        }

        public CreateInventoryRequest(Long productId, Integer quantity, Long warehouseId) {
            this.productId = productId;
            this.quantity = quantity;
            this.warehouseId = warehouseId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }
    }

    private static class MoveInventoryRequest {
        private Long productId;
        private InventoryState fromState;
        private InventoryState toState;
        private Integer quantity;
        private Long warehouseId;

        public MoveInventoryRequest() {
        }

        public MoveInventoryRequest(Long productId, InventoryState fromState, InventoryState toState, Integer quantity, Long warehouseId) {
            this.productId = productId;
            this.fromState = fromState;
            this.toState = toState;
            this.quantity = quantity;
            this.warehouseId = warehouseId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public InventoryState getFromState() {
            return fromState;
        }

        public void setFromState(InventoryState fromState) {
            this.fromState = fromState;
        }

        public InventoryState getToState() {
            return toState;
        }

        public void setToState(InventoryState toState) {
            this.toState = toState;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }
    }
}
