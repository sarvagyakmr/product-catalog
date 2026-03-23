package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.WarehouseCreateRequest;
import com.example.ordermanagement.entity.Warehouse;
import com.example.ordermanagement.repository.WarehouseRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public Warehouse createWarehouse(WarehouseCreateRequest request) {
        Warehouse warehouse = new Warehouse(
            request.getName(),
            request.getCode(),
            request.getType(),
            request.getAddress()
        );
        return warehouseRepository.save(warehouse);
    }

    public Optional<Warehouse> getWarehouseById(Long id) {
        return warehouseRepository.findById(id);
    }

    public Iterable<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }
}
