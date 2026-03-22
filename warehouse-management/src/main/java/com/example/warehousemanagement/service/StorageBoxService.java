package com.example.warehousemanagement.service;

import com.example.warehousemanagement.dto.StorageBoxCreateRequest;
import com.example.warehousemanagement.entity.StorageBox;
import com.example.warehousemanagement.enums.BoxType;
import com.example.warehousemanagement.repository.StorageBoxRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StorageBoxService {

    private final StorageBoxRepository storageBoxRepository;

    public StorageBoxService(StorageBoxRepository storageBoxRepository) {
        this.storageBoxRepository = storageBoxRepository;
    }

    public StorageBox createStorageBox(StorageBoxCreateRequest request) {
        if (request.getWarehouseId() == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        StorageBox storageBox = new StorageBox(request.getType(), request.getGateEntryId(), null, request.getWarehouseId());
        return storageBoxRepository.save(storageBox);
    }

    public Optional<StorageBox> getStorageBoxById(Long id) {
        return storageBoxRepository.findById(id);
    }
}
