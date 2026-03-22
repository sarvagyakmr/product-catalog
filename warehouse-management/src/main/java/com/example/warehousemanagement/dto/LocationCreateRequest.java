package com.example.warehousemanagement.dto;

public class LocationCreateRequest {
    private String aisle;
    private String displayName;
    private Long warehouseId;

    public LocationCreateRequest() {
    }

    public LocationCreateRequest(String aisle, String displayName) {
        this.aisle = aisle;
        this.displayName = displayName;
    }

    public LocationCreateRequest(String aisle, String displayName, Long warehouseId) {
        this.aisle = aisle;
        this.displayName = displayName;
        this.warehouseId = warehouseId;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
