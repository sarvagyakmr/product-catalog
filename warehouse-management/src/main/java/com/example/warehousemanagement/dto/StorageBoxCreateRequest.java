package com.example.warehousemanagement.dto;

import com.example.warehousemanagement.enums.BoxType;

public class StorageBoxCreateRequest {
    private BoxType type;
    private Long gateEntryId;

    public StorageBoxCreateRequest() {
    }

    public StorageBoxCreateRequest(BoxType type, Long gateEntryId) {
        this.type = type;
        this.gateEntryId = gateEntryId;
    }

    public BoxType getType() {
        return type;
    }

    public void setType(BoxType type) {
        this.type = type;
    }

    public Long getGateEntryId() {
        return gateEntryId;
    }

    public void setGateEntryId(Long gateEntryId) {
        this.gateEntryId = gateEntryId;
    }
}
