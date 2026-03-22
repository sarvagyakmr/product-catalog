package com.example.warehousemanagement.dto;

import com.example.warehousemanagement.enums.BoxType;

public class StorageBoxCreateRequest {
    private BoxType type;
    private Long gateEntryId;
    private Long warehouseId;

    public StorageBoxCreateRequest() {
    }

    public StorageBoxCreateRequest(BoxType type, Long gateEntryId) {
        this.type = type;
        this.gateEntryId = gateEntryId;
    }

    public StorageBoxCreateRequest(BoxType type, Long gateEntryId, Long warehouseId) {
        this.type = type;
        this.gateEntryId = gateEntryId;
        this.warehouseId = warehouseId;
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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
