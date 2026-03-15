package com.example.warehousemanagement.dto;

public class PutAwayRequest {
    private Long boxId;
    private Long locationId;

    public PutAwayRequest() {
    }

    public PutAwayRequest(Long boxId, Long locationId) {
        this.boxId = boxId;
        this.locationId = locationId;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}
