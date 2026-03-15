package com.example.warehousemanagement.dto;

import java.util.List;

public class CycleCountRequest {
    private Long boxId;
    private List<Long> itemIds;

    public CycleCountRequest() {
    }

    public CycleCountRequest(Long boxId, List<Long> itemIds) {
        this.boxId = boxId;
        this.itemIds = itemIds;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }

    public List<Long> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Long> itemIds) {
        this.itemIds = itemIds;
    }
}
