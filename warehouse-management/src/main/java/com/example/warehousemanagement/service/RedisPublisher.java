package com.example.warehousemanagement.service;

import com.example.commons.dto.InventoryEventDto;
import com.example.warehousemanagement.dto.PickListCreateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisPublisher {

    private static final String ALLOCATED_ORDERS_CHANNEL = "allocated-orders";
    private static final String INVENTORY_EVENTS_CHANNEL = "inventory-events";
    private static final String PICKLIST_CREATE_CHANNEL = "picklist-create";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisPublisher(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishInventoryEvent(InventoryEventDto event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(INVENTORY_EVENTS_CHANNEL, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize inventory event for Redis", e);
        }
    }

    public void publishPickListCreate(PickListCreateEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(PICKLIST_CREATE_CHANNEL, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize picklist create event for Redis", e);
        }
    }
}
