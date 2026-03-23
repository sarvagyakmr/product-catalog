package com.example.ordermanagement.dto;

import com.example.commons.enums.InventoryState;

public class InventoryMoveRequest {
    private Long productId;
    private InventoryState fromState;
    private InventoryState toState;
    private Integer quantity;

    public InventoryMoveRequest() {
    }

    public InventoryMoveRequest(Long productId, InventoryState fromState, InventoryState toState, Integer quantity) {
        this.productId = productId;
        this.fromState = fromState;
        this.toState = toState;
        this.quantity = quantity;
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
}
