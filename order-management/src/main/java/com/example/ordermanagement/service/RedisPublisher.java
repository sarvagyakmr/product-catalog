package com.example.ordermanagement.service;

import com.example.commons.dto.InventoryEventDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisPublisher {

    private static final String ALLOCATED_ORDERS_CHANNEL = "allocated-orders";
    private static final String INVENTORY_EVENTS_CHANNEL = "inventory-events";
    private static final String CANCELLED_ORDERS_CHANNEL = "cancelled-orders";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisPublisher(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishAllocatedOrder(Object order) {
        try {
            String message = objectMapper.writeValueAsString(order);
            stringRedisTemplate.convertAndSend(ALLOCATED_ORDERS_CHANNEL, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order for Redis", e);
        }
    }

    public void publishInventoryEvent(InventoryEventDto event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(INVENTORY_EVENTS_CHANNEL, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize inventory event for Redis", e);
        }
    }

    public void publishCancelledOrder(Object order) {
        try {
            String message = objectMapper.writeValueAsString(order);
            stringRedisTemplate.convertAndSend(CANCELLED_ORDERS_CHANNEL, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cancelled order for Redis", e);
        }
    }
}
