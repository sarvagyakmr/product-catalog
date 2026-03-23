package com.example.warehousemanagement.service;

import com.example.warehousemanagement.client.OrderManagementClient;
import com.example.warehousemanagement.dto.OutwardOrderDto;
import com.example.warehousemanagement.dto.OutwardOrderItemDto;
import com.example.warehousemanagement.entity.OutwardOrder;
import com.example.warehousemanagement.entity.OutwardOrderItem;
import com.example.warehousemanagement.enums.OutwardOrderStatus;
import com.example.warehousemanagement.repository.OutwardOrderItemRepository;
import com.example.warehousemanagement.repository.OutwardOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RedisSubscriber {

    private final OutwardOrderRepository outwardOrderRepository;
    private final OutwardOrderItemRepository outwardOrderItemRepository;
    private final OrderManagementClient orderManagementClient;
    private final PickListService pickListService;
    private final ObjectMapper objectMapper;

    public RedisSubscriber(OutwardOrderRepository outwardOrderRepository,
                           OutwardOrderItemRepository outwardOrderItemRepository,
                           OrderManagementClient orderManagementClient,
                           PickListService pickListService,
                           ObjectMapper objectMapper) {
        this.outwardOrderRepository = outwardOrderRepository;
        this.outwardOrderItemRepository = outwardOrderItemRepository;
        this.orderManagementClient = orderManagementClient;
        this.pickListService = pickListService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void onMessage(String message, String channel) {
        try {
            if ("allocated-orders".equals(channel)) {
                OutwardOrderDto dto = objectMapper.readValue(message, OutwardOrderDto.class);
                processAllocatedOrder(dto);
            } else if ("cancelled-orders".equals(channel)) {
                OutwardOrderDto dto = objectMapper.readValue(message, OutwardOrderDto.class);
                processCancelledOrder(dto);
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

        // Create items
        if (dto.getItems() != null) {
            for (OutwardOrderItemDto itemDto : dto.getItems()) {
                OutwardOrderItem item = new OutwardOrderItem(
                    localOrder.getId(),
                    itemDto.getProductId(),
                    itemDto.getAllocatedQuantity() != null ? itemDto.getAllocatedQuantity() : itemDto.getOrderedQuantity()
                );
                outwardOrderItemRepository.save(item);
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
