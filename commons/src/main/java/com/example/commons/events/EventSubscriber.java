package com.example.commons.events;

/**
 * Interface for subscribing to events from a message bus (e.g., Redis pub/sub).
 * Implementations receive deserialized messages for processing.
 * <p>
 * The method signature {@code onMessage(String message, String channel)}
 * is compatible with Spring's {@code MessageListenerAdapter}.
 */
@FunctionalInterface
public interface EventSubscriber {

    /**
     * Handle an incoming message from the specified channel.
     *
     * @param message the raw message payload (typically JSON)
     * @param channel the channel/topic the message was received on
     */
    void onMessage(String message, String channel);
}
