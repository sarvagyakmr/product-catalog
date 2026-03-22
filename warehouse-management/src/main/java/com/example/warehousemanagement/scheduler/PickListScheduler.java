package com.example.warehousemanagement.scheduler;

import com.example.warehousemanagement.service.PickListService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PickListScheduler {

    private final PickListService pickListService;

    public PickListScheduler(PickListService pickListService) {
        this.pickListService = pickListService;
    }

    @Scheduled(fixedRate = 5000)
    public void createPickLists() {
        pickListService.createPickLists();
    }
}
