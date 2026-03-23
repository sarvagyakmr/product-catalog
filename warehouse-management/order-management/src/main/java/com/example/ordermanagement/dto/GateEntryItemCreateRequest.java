package com.example.ordermanagement.dto;

public class GateEntryItemCreateRequest {
    private Long productId;
    private Integer quantity;

    public GateEntryItemCreateRequest() {
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
}
