package com.example.warehousemanagement.service;

import com.example.warehousemanagement.dto.LocationCreateRequest;
import com.example.warehousemanagement.entity.Location;
import com.example.warehousemanagement.repository.LocationRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Location createLocation(LocationCreateRequest request) {
        Location location = new Location(request.getAisle(), request.getDisplayName());
        return locationRepository.save(location);
    }

    public Optional<Location> getLocationByAisle(String aisle) {
        return locationRepository.findByAisle(aisle);
    }

    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }
}
