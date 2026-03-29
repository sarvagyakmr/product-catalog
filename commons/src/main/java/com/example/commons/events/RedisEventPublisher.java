package com.example.commons.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-based implementation of {@link EventPublisher}.
 * Uses Spring Data Redis {@link StringRedisTemplate} for pub/sub.
 */
@Component
public class RedisEventPublisher implements EventPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisEventPublisher(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String channel, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            publishRaw(channel, json);
        } catch (JsonProcessingException e) {
            throw new EventPublishException("Failed to serialize message for channel: " + channel, e);
        }
    }

    @Override
    public void publishRaw(String channel, String jsonMessage) {
        stringRedisTemplate.convertAndSend(channel, jsonMessage);
    }

    /**
     * Exception thrown when publishing an event fails.
     */
    public static class EventPublishException extends RuntimeException {
        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
