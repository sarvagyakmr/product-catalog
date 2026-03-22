package com.example.ordermanagement.scheduler;

import com.example.ordermanagement.entity.OutwardOrder;
import com.example.ordermanagement.enums.OutwardOrderStatus;
import com.example.ordermanagement.service.OutwardOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryAllocationScheduler {

    private final OutwardOrderService outwardOrderService;

    public InventoryAllocationScheduler(OutwardOrderService outwardOrderService) {
        this.outwardOrderService = outwardOrderService;
    }

    @Scheduled(fixedRate = 5000)
    public void allocateInventory() {
        Iterable<OutwardOrder> createdOrders = outwardOrderService.getOrdersByStatus(OutwardOrderStatus.CREATED);
        for (OutwardOrder order : createdOrders) {
            try {
                outwardOrderService.allocateInventoryForOrder(order);
            } catch (Exception e) {
                // Log error but continue with other orders
                // In production, use proper logging
            }
        }
    }
}
