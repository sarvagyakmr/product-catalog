package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.GateEntryCreateRequest;
import com.example.ordermanagement.entity.GateEntry;
import com.example.ordermanagement.entity.InwardOrder;
import com.example.ordermanagement.enums.GateEntryStatus;
import com.example.ordermanagement.enums.InwardOrderStatus;
import com.example.ordermanagement.repository.GateEntryRepository;
import com.example.ordermanagement.repository.InwardOrderRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GateEntryService {

    private final GateEntryRepository gateEntryRepository;
    private final InwardOrderRepository inwardOrderRepository;

    public GateEntryService(GateEntryRepository gateEntryRepository, InwardOrderRepository inwardOrderRepository) {
        this.gateEntryRepository = gateEntryRepository;
        this.inwardOrderRepository = inwardOrderRepository;
    }

    public GateEntry createGateEntry(GateEntryCreateRequest request) {
        InwardOrder order = inwardOrderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Inward order not found with id: " + request.getOrderId()));
        
        // Update order status to PROCESSING
        order.setStatus(InwardOrderStatus.PROCESSING);
        inwardOrderRepository.save(order);
        
        // Create gate entry with PROCESSING status so items can be added
        GateEntry gateEntry = new GateEntry(request.getOrderId(), GateEntryStatus.PROCESSING);
        return gateEntryRepository.save(gateEntry);
    }

    public Optional<GateEntry> getGateEntryById(Long id) {
        return gateEntryRepository.findById(id);
    }

    public GateEntry updateGateEntry(GateEntry gateEntry) {
        return gateEntryRepository.save(gateEntry);
    }
}
