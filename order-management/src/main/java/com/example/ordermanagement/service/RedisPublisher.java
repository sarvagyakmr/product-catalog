package com.example.ordermanagement.service;

import com.example.commons.dto.InventoryEventDto;
import com.example.commons.events.EventNames;
import com.example.commons.events.EventPublisher;
import org.springframework.stereotype.Service;

/**
 * Redis publisher for order-management events.
 * Delegates to {@link EventPublisher} from commons module.
 */
@Service
public class RedisPublisher {

    private final EventPublisher eventPublisher;

    public RedisPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishAllocatedOrder(Object order) {
        eventPublisher.publish(EventNames.ALLOCATED_ORDERS, order);
    }

    public void publishInventoryEvent(InventoryEventDto event) {
        eventPublisher.publish(EventNames.INVENTORY_EVENTS, event);
    }

    public void publishCancelledOrder(Object order) {
        eventPublisher.publish(EventNames.CANCELLED_ORDERS, order);
    }
}
