package com.example.warehousemanagement.dto;

public class LocationCreateRequest {
    private String aisle;
    private String displayName;

    public LocationCreateRequest() {
    }

    public LocationCreateRequest(String aisle, String displayName) {
        this.aisle = aisle;
        this.displayName = displayName;
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
}
