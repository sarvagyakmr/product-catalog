package com.example.warehousemanagement.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class OutwardOrderDto {
    private Long id;
    private String channelOrderId;
    private String channel;
    private Long warehouseId;
    private String status;
    private OffsetDateTime timestamp;
    private List<OutwardOrderItemDto> items;

    public OutwardOrderDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelOrderId() {
        return channelOrderId;
    }

    public void setChannelOrderId(String channelOrderId) {
        this.channelOrderId = channelOrderId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<OutwardOrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OutwardOrderItemDto> items) {
        this.items = items;
    }
}
