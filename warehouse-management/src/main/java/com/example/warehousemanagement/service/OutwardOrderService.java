package com.example.warehousemanagement.service;

import com.example.warehousemanagement.client.OrderManagementClient;
import com.example.warehousemanagement.dto.OutwardOrderDto;
import com.example.warehousemanagement.dto.OutwardOrderItemDto;
import com.example.warehousemanagement.dto.PickListCreateEvent;
import com.example.warehousemanagement.entity.OutwardOrder;
import com.example.warehousemanagement.entity.OutwardOrderItem;
import com.example.warehousemanagement.enums.OutwardOrderStatus;
import com.example.warehousemanagement.repository.OutwardOrderItemRepository;
import com.example.warehousemanagement.repository.OutwardOrderRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutwardOrderService {

    private final OutwardOrderRepository outwardOrderRepository;
    private final OutwardOrderItemRepository outwardOrderItemRepository;
    private final OrderManagementClient orderManagementClient;
    private final RedisPublisher redisPublisher;

    public OutwardOrderService(OutwardOrderRepository outwardOrderRepository,
                                OutwardOrderItemRepository outwardOrderItemRepository,
                                OrderManagementClient orderManagementClient,
                                RedisPublisher redisPublisher) {
        this.outwardOrderRepository = outwardOrderRepository;
        this.outwardOrderItemRepository = outwardOrderItemRepository;
        this.orderManagementClient = orderManagementClient;
        this.redisPublisher = redisPublisher;
    }

    public Optional<OutwardOrder> getOutwardOrderById(Long id) {
        return outwardOrderRepository.findById(id);
    }

    public List<OutwardOrderItem> getItemsByOrderId(Long orderId) {
        return outwardOrderItemRepository.findByOrderId(orderId);
    }

    @Transactional
    public void syncAllocatedOrders() {
        List<OutwardOrderDto> allocatedOrders = orderManagementClient.getOutwardOrdersByStatus("ALLOCATED");

        for (OutwardOrderDto dto : allocatedOrders) {
            // Check if already exists locally
            Optional<OutwardOrder> existing = outwardOrderRepository.findByOrderManagementId(dto.getId());
            if (existing.isPresent()) {
                continue; // Already synced
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

            // Create items from DTO
            // The list endpoint returns OutwardOrderResponse with items, but single-order doesn't
            // So fetch items via dedicated endpoint if not in DTO
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
    }
}
