package com.example.warehousemanagement.controller;

import com.example.warehousemanagement.dto.LocationCreateRequest;
import com.example.warehousemanagement.entity.Location;
import com.example.warehousemanagement.service.LocationService;
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
@RequestMapping("/api/storage-locations")
public class StorageLocationController {

    private final LocationService locationService;

    public StorageLocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    public ResponseEntity<Location> createLocation(@RequestBody LocationCreateRequest request) {
        Location created = locationService.createLocation(request);
        return ResponseEntity.created(URI.create("/api/storage-locations/" + created.getId()))
            .body(created);
    }

    @GetMapping("/aisle/{aisle}")
    public ResponseEntity<Location> getLocationByAisle(@PathVariable("aisle") String aisle) {
        Optional<Location> location = locationService.getLocationByAisle(aisle);
        return location.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocationById(@PathVariable("id") Long id) {
        Optional<Location> location = locationService.getLocationById(id);
        return location.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
