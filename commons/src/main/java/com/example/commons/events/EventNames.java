package com.example.commons.events;

/**
 * Constants for Redis pub/sub event channel names.
 * Centralize all event names here to avoid magic strings across modules.
 */
public final class EventNames {

    private EventNames() {
        // Prevent instantiation
    }

    // Inventory events
    public static final String INVENTORY_EVENTS = "inventory-events";

    // Order events
    public static final String ALLOCATED_ORDERS = "allocated-orders";
    public static final String CANCELLED_ORDERS = "cancelled-orders";

    // Picklist events
    public static final String PICKLIST_CREATE = "picklist-create";

    // Add more event channel names as needed
}
