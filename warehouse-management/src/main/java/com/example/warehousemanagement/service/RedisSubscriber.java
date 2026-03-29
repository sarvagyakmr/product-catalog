package com.example.warehousemanagement.service;

import com.example.commons.events.EventNames;
import com.example.commons.client.OrderManagementClient;
import com.example.commons.dto.OutwardOrderDto;
import com.example.commons.events.RedisEventSubscriber;
import com.example.commons.dto.OutwardOrderItemDto;
import com.example.warehousemanagement.dto.PickListCreateEvent;
import com.example.warehousemanagement.entity.OutwardOrder;
import com.example.warehousemanagement.entity.OutwardOrderItem;
import com.example.commons.enums.OutwardOrderStatus;
import com.example.warehousemanagement.repository.OutwardOrderItemRepository;
import com.example.warehousemanagement.repository.OutwardOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Redis subscriber for warehouse-management events.
 * Extends {@link RedisEventSubscriber} from commons module.
 */
@Service
public class RedisSubscriber extends RedisEventSubscriber {

    private final OutwardOrderRepository outwardOrderRepository;
    private final OutwardOrderItemRepository outwardOrderItemRepository;
    private final OrderManagementClient orderManagementClient;
    private final PickListService pickListService;
    private final RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper;

    public RedisSubscriber(OutwardOrderRepository outwardOrderRepository,
                           OutwardOrderItemRepository outwardOrderItemRepository,
                           OrderManagementClient orderManagementClient,
                           PickListService pickListService,
                           RedisPublisher redisPublisher,
                           ObjectMapper objectMapper) {
        this.outwardOrderRepository = outwardOrderRepository;
        this.outwardOrderItemRepository = outwardOrderItemRepository;
        this.orderManagementClient = orderManagementClient;
        this.pickListService = pickListService;
        this.redisPublisher = redisPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void onMessage(String message, String channel) {
        try {
            if (EventNames.ALLOCATED_ORDERS.equals(channel)) {
                OutwardOrderDto dto = objectMapper.readValue(message, OutwardOrderDto.class);
                processAllocatedOrder(dto);
            } else if (EventNames.CANCELLED_ORDERS.equals(channel)) {
                OutwardOrderDto dto = objectMapper.readValue(message, OutwardOrderDto.class);
                processCancelledOrder(dto);
            } else if (EventNames.PICKLIST_CREATE.equals(channel)) {
                PickListCreateEvent event = objectMapper.readValue(message, PickListCreateEvent.class);
                pickListService.createPickListForItem(event.getOrderId(), event.getProductId(), event.getQuantity());
            }
        } catch (JsonProcessingException e) {
            // Log error but don't fail
        }
    }

    private void processAllocatedOrder(OutwardOrderDto dto) {
        // Check if already exists locally
        Optional<OutwardOrder> existing = outwardOrderRepository.findByOrderManagementId(dto.getId());
        if (existing.isPresent()) {
            return; // Already synced
        }

        // Create local copy
        OutwardOrder localOrder = new OutwardOrder(
            dto.getId(),
            dto.getChannelOrderId(),
            dto.getChannel(),
            dto.getWarehouseId(),
            OutwardOrderStatus.ALLOCATED
        );
        localOrder = outwardOrderRepository.save(localOrder);

        // Create items - fetch from OMS (Redis events don't include items)
        // The single-order endpoint returns entity without items, so use dedicated items endpoint
        List<OutwardOrderItemDto> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            // Fetch items via dedicated endpoint
            items = orderManagementClient.getOutwardOrderItems(dto.getId());
        }

        if (items != null) {
            for (OutwardOrderItemDto itemDto : items) {
                Integer quantity = itemDto.getAllocatedQuantity() != null ? itemDto.getAllocatedQuantity() : itemDto.getOrderedQuantity();
                OutwardOrderItem item = new OutwardOrderItem(
                    localOrder.getId(),
                    itemDto.getProductId(),
                    quantity
                );
                outwardOrderItemRepository.save(item);

                // Publish picklist create event for this item
                PickListCreateEvent event = new PickListCreateEvent(
                    localOrder.getId(),
                    itemDto.getProductId(),
                    quantity
                );
                redisPublisher.publishPickListCreate(event);
            }
        }

        // Update status to PROCESSING in order management
        orderManagementClient.updateOutwardOrderStatus(dto.getId(), OutwardOrderStatus.PROCESSING);
    }

    private void processCancelledOrder(OutwardOrderDto dto) {
        // Find the local order by order management id
        Optional<OutwardOrder> existingOpt = outwardOrderRepository.findByOrderManagementId(dto.getId());
        if (existingOpt.isEmpty()) {
            return; // Order not found locally, nothing to cancel
        }

        OutwardOrder localOrder = existingOpt.get();

        // Cancel picklists and handle picked items
        pickListService.cancelPickListsForOrder(localOrder.getId());

        // Update local order status to CANCELLED
        localOrder.setStatus(OutwardOrderStatus.CANCELLED);
        outwardOrderRepository.save(localOrder);
    }
}
