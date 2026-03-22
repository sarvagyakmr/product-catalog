package com.example.warehousemanagement.service;

import com.example.warehousemanagement.client.OrderManagementClient;
import com.example.warehousemanagement.dto.OutwardOrderDto;
import com.example.warehousemanagement.dto.OutwardOrderItemDto;
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

    public OutwardOrderService(OutwardOrderRepository outwardOrderRepository,
                                OutwardOrderItemRepository outwardOrderItemRepository,
                                OrderManagementClient orderManagementClient) {
        this.outwardOrderRepository = outwardOrderRepository;
        this.outwardOrderItemRepository = outwardOrderItemRepository;
        this.orderManagementClient = orderManagementClient;
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
    }
}
