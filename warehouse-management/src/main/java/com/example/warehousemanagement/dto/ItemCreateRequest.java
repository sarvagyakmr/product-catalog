package com.example.warehousemanagement.dto;

public class ItemCreateRequest {
    private Long productId;
    private Long warehouseId;

    public ItemCreateRequest() {
    }

    public ItemCreateRequest(Long productId) {
        this.productId = productId;
    }

    public ItemCreateRequest(Long productId, Long warehouseId) {
        this.productId = productId;
        this.warehouseId = warehouseId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
