package com.example.ordermanagement.service;

import com.example.commons.client.ProductCatalogClient;
import com.example.commons.dto.InventoryEventDto;
import com.example.ordermanagement.dto.GateEntryItemCreateRequest;
import com.example.ordermanagement.entity.GateEntry;
import com.example.ordermanagement.entity.GateEntryItem;
import com.example.ordermanagement.entity.InwardOrder;
import com.example.ordermanagement.enums.GateEntryStatus;
import com.example.ordermanagement.repository.GateEntryItemRepository;
import com.example.ordermanagement.repository.GateEntryRepository;
import com.example.ordermanagement.repository.InwardOrderRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GateEntryItemService {

    private final GateEntryItemRepository gateEntryItemRepository;
    private final GateEntryRepository gateEntryRepository;
    private final InwardOrderRepository inwardOrderRepository;
    private final ProductCatalogClient productCatalogClient;
    private final RedisPublisher redisPublisher;

    public GateEntryItemService(GateEntryItemRepository gateEntryItemRepository,
                                GateEntryRepository gateEntryRepository,
                                InwardOrderRepository inwardOrderRepository,
                                ProductCatalogClient productCatalogClient,
                                RedisPublisher redisPublisher) {
        this.gateEntryItemRepository = gateEntryItemRepository;
        this.gateEntryRepository = gateEntryRepository;
        this.inwardOrderRepository = inwardOrderRepository;
        this.productCatalogClient = productCatalogClient;
        this.redisPublisher = redisPublisher;
    }

    @Transactional
    public GateEntryItem addGateEntryItem(Long gateEntryId, GateEntryItemCreateRequest request) {
        GateEntry gateEntry = gateEntryRepository.findById(gateEntryId)
            .orElseThrow(() -> new IllegalArgumentException("Gate entry not found with id: " + gateEntryId));

        if (gateEntry.getStatus() != GateEntryStatus.PROCESSING) {
            throw new IllegalStateException("Gate entry must be in PROCESSING status to add items. Current status: " + gateEntry.getStatus());
        }

        productCatalogClient.getProduct(request.getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + request.getProductId()));

        GateEntryItem gateEntryItem = new GateEntryItem(gateEntryId, request.getProductId(), request.getQuantity());
        GateEntryItem savedItem = gateEntryItemRepository.save(gateEntryItem);

        // Get warehouseId from the associated InwardOrder
        InwardOrder inwardOrder = inwardOrderRepository.findById(gateEntry.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Inward order not found with id: " + gateEntry.getOrderId()));
        Long warehouseId = inwardOrder.getWarehouseId();

        // Publish inventory create event to Redis
        InventoryEventDto event = InventoryEventDto.createEvent(
            request.getProductId(),
            request.getQuantity(),
            warehouseId
        );
        redisPublisher.publishInventoryEvent(event);

        return savedItem;
    }

    public Optional<GateEntryItem> getGateEntryItemById(Long id) {
        return gateEntryItemRepository.findById(id);
    }
}
