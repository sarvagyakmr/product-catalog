package com.example.warehousemanagement.dto;

public class GateEntryCreateRequest {
    private Long inwardOrderId;

    public GateEntryCreateRequest() {
    }

    public GateEntryCreateRequest(Long inwardOrderId) {
        this.inwardOrderId = inwardOrderId;
    }

    public Long getInwardOrderId() {
        return inwardOrderId;
    }

    public void setInwardOrderId(Long inwardOrderId) {
        this.inwardOrderId = inwardOrderId;
    }
}
