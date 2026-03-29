package com.example.warehousemanagement.service;

import com.example.commons.service.AbstractService;
import com.example.warehousemanagement.dto.LocationCreateRequest;
import com.example.warehousemanagement.entity.Location;
import com.example.warehousemanagement.repository.LocationRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LocationService extends AbstractService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Location createLocation(LocationCreateRequest request) {
        checkNotNull(request.getWarehouseId(), "warehouseId");
        Location location = new Location(request.getAisle(), request.getDisplayName(), request.getWarehouseId());
        return locationRepository.save(location);
    }

    public Optional<Location> getLocationByAisle(String aisle) {
        return locationRepository.findByAisle(aisle);
    }

    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }
}
