package com.example.warehousemanagement.service;

import com.example.warehousemanagement.client.OrderManagementClient;
import com.example.warehousemanagement.dto.GateEntryCreateRequest;
import com.example.warehousemanagement.entity.GateEntry;
import com.example.warehousemanagement.repository.GateEntryRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GateEntryService {

    private final GateEntryRepository gateEntryRepository;
    private final OrderManagementClient orderManagementClient;

    public GateEntryService(GateEntryRepository gateEntryRepository, OrderManagementClient orderManagementClient) {
        this.gateEntryRepository = gateEntryRepository;
        this.orderManagementClient = orderManagementClient;
    }

    @Transactional
    public GateEntry createGateEntry(GateEntryCreateRequest request) {
        GateEntry gateEntry = new GateEntry(request.getInwardOrderId());
        GateEntry saved = gateEntryRepository.save(gateEntry);

        // Call order management API to create gate entry there as well
        try {
            orderManagementClient.createGateEntry(request.getInwardOrderId());
        } catch (Exception e) {
            // Log the error but don't fail the operation
            // In production, this might need retry logic or compensation
        }

        return saved;
    }

    public Optional<GateEntry> getGateEntryById(Long id) {
        return gateEntryRepository.findById(id);
    }
}
