package com.example.warehousemanagement.dto;

public class ItemCreateRequest {
    private Long productId;

    public ItemCreateRequest() {
    }

    public ItemCreateRequest(Long productId) {
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
