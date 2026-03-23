package com.example.inventory.service;

import com.example.commons.dto.InventoryEventDto;
import com.example.commons.enums.InventoryState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryEventSubscriber {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public InventoryEventSubscriber(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void onMessage(String message, String channel) {
        if (!"inventory-events".equals(channel)) {
            return;
        }
        try {
            InventoryEventDto event = objectMapper.readValue(message, InventoryEventDto.class);
            processInventoryEvent(event);
        } catch (JsonProcessingException e) {
            // Log error but don't fail
        }
    }

    private void processInventoryEvent(InventoryEventDto event) {
        switch (event.getEventType()) {
            case CREATE:
                inventoryService.createInventory(
                    event.getProductId(),
                    event.getQuantity(),
                    event.getWarehouseId()
                );
                break;
            case MOVE:
                inventoryService.moveInventory(
                    event.getProductId(),
                    event.getFromState(),
                    event.getToState(),
                    event.getQuantity(),
                    event.getWarehouseId()
                );
                break;
        }
    }
}
