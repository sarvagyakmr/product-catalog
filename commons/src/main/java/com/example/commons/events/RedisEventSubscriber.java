package com.example.commons.events;

/**
 * Base class for Redis pub/sub subscribers.
 * <p>
 * Subclasses should override {@link #onMessage(String, String)} to handle messages.
 * This class is compatible with Spring's {@code MessageListenerAdapter}.
 * <p>
 * Example usage:
 * <pre>
 * &#64;Component
 * public class MyEventSubscriber extends RedisEventSubscriber {
 *     &#64;Override
 *     public void onMessage(String message, String channel) {
 *         // handle message
 *     }
 * }
 * </pre>
 */
public abstract class RedisEventSubscriber implements EventSubscriber {

    /**
     * Called when a message is received on a subscribed channel.
     * <p>
     * This method signature is compatible with Spring's {@code MessageListenerAdapter}
     * when configured with method name "onMessage".
     *
     * @param message the raw message payload (typically JSON)
     * @param channel the channel/topic the message was received on
     */
    @Override
    public abstract void onMessage(String message, String channel);
}
