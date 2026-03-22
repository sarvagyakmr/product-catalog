package com.example.warehousemanagement.scheduler;

import com.example.warehousemanagement.service.OutwardOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutwardOrderScheduler {

    private final OutwardOrderService outwardOrderService;

    public OutwardOrderScheduler(OutwardOrderService outwardOrderService) {
        this.outwardOrderService = outwardOrderService;
    }

    @Scheduled(fixedRate = 5000)
    public void syncAllocatedOrders() {
        outwardOrderService.syncAllocatedOrders();
    }
}
