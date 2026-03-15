package com.example.warehousemanagement.dto;

public class AddItemToBoxRequest {
    private Long itemId;
    private Long boxId;

    public AddItemToBoxRequest() {
    }

    public AddItemToBoxRequest(Long itemId, Long boxId) {
        this.itemId = itemId;
        this.boxId = boxId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }
}
