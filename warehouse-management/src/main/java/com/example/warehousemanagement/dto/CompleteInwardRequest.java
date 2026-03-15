package com.example.warehousemanagement.dto;

public class CompleteInwardRequest {
    private Long boxId;

    public CompleteInwardRequest() {
    }

    public CompleteInwardRequest(Long boxId) {
        this.boxId = boxId;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }
}
