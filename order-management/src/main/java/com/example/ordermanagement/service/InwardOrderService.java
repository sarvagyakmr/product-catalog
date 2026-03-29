package com.example.ordermanagement.service;

import com.example.commons.service.AbstractService;
import com.example.ordermanagement.dto.InwardOrderCreateRequest;
import com.example.ordermanagement.entity.InwardOrder;
import com.example.ordermanagement.enums.InwardOrderStatus;
import com.example.ordermanagement.repository.InwardOrderRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class InwardOrderService extends AbstractService {

    private final InwardOrderRepository inwardOrderRepository;

    public InwardOrderService(InwardOrderRepository inwardOrderRepository) {
        this.inwardOrderRepository = inwardOrderRepository;
    }

    public InwardOrder createInwardOrder(InwardOrderCreateRequest request) {
        checkNotNull(request.getWarehouseId(), "warehouseId");
        InwardOrder inwardOrder = new InwardOrder(
            request.getChannelOrderId(),
            request.getChannel(),
            OffsetDateTime.now(),
            InwardOrderStatus.CREATED,
            request.getWarehouseId()
        );
        return inwardOrderRepository.save(inwardOrder);
    }

    public Optional<InwardOrder> getInwardOrderById(Long id) {
        return inwardOrderRepository.findById(id);
    }
}
