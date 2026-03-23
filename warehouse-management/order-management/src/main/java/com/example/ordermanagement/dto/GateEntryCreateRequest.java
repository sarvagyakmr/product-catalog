package com.example.ordermanagement.dto;

public class GateEntryCreateRequest {
    private Long orderId;

    public GateEntryCreateRequest() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
