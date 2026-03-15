package com.example.warehousemanagement.controller;

import com.example.warehousemanagement.dto.StorageBoxCreateRequest;
import com.example.warehousemanagement.entity.StorageBox;
import com.example.warehousemanagement.service.StorageBoxService;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage-boxes")
public class StorageBoxController {

    private final StorageBoxService storageBoxService;

    public StorageBoxController(StorageBoxService storageBoxService) {
        this.storageBoxService = storageBoxService;
    }

    @PostMapping
    public ResponseEntity<StorageBox> createStorageBox(@RequestBody StorageBoxCreateRequest request) {
        StorageBox created = storageBoxService.createStorageBox(request);
        return ResponseEntity.created(URI.create("/api/storage-boxes/" + created.getId()))
            .body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StorageBox> getStorageBoxById(@PathVariable("id") Long id) {
        Optional<StorageBox> storageBox = storageBoxService.getStorageBoxById(id);
        return storageBox.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
