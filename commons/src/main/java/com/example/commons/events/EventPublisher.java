package com.example.commons.events;

/**
 * Interface for publishing events to a message bus (e.g., Redis pub/sub).
 * Implementations handle serialization and transport details.
 */
public interface EventPublisher {

    /**
     * Publish a message to the specified channel.
     *
     * @param channel the channel/topic name (use constants from EventNames)
     * @param message the message object to publish (will be serialized to JSON)
     */
    void publish(String channel, Object message);

    /**
     * Publish a raw JSON string to the specified channel.
     *
     * @param channel the channel/topic name
     * @param jsonMessage the pre-serialized JSON message
     */
    void publishRaw(String channel, String jsonMessage);
}
