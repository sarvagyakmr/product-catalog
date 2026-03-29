package com.example.warehousemanagement.service;

import com.example.commons.dto.InventoryEventDto;
import com.example.commons.events.EventNames;
import com.example.commons.events.EventPublisher;
import com.example.warehousemanagement.dto.PickListCreateEvent;
import org.springframework.stereotype.Service;

/**
 * Redis publisher for warehouse-management events.
 * Delegates to {@link EventPublisher} from commons module.
 */
@Service
public class RedisPublisher {

    private final EventPublisher eventPublisher;

    public RedisPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishInventoryEvent(InventoryEventDto event) {
        eventPublisher.publish(EventNames.INVENTORY_EVENTS, event);
    }

    public void publishPickListCreate(PickListCreateEvent event) {
        eventPublisher.publish(EventNames.PICKLIST_CREATE, event);
    }
}
