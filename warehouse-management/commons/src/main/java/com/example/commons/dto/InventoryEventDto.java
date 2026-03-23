package com.example.commons.dto;

import com.example.commons.enums.InventoryState;

/**
 * DTO for inventory events published to Redis.
 * Used for communication between OMS/WMS and Inventory Service.
 */
public class InventoryEventDto {

    private EventType eventType;
    private Long productId;
    private Integer quantity;
    private Long warehouseId;
    private InventoryState fromState;
    private InventoryState toState;

    public InventoryEventDto() {
    }

    public enum EventType {
        CREATE,
        MOVE
    }

    public static InventoryEventDto createEvent(Long productId, Integer quantity, Long warehouseId) {
        InventoryEventDto dto = new InventoryEventDto();
        dto.setEventType(EventType.CREATE);
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        dto.setWarehouseId(warehouseId);
        return dto;
    }

    public static InventoryEventDto moveEvent(Long productId, InventoryState fromState, InventoryState toState, Integer quantity, Long warehouseId) {
        InventoryEventDto dto = new InventoryEventDto();
        dto.setEventType(EventType.MOVE);
        dto.setProductId(productId);
        dto.setFromState(fromState);
        dto.setToState(toState);
        dto.setQuantity(quantity);
        dto.setWarehouseId(warehouseId);
        return dto;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public InventoryState getFromState() {
        return fromState;
    }

    public void setFromState(InventoryState fromState) {
        this.fromState = fromState;
    }

    public InventoryState getToState() {
        return toState;
    }

    public void setToState(InventoryState toState) {
        this.toState = toState;
    }
}
