package com.example.warehousemanagement.repository;

import com.example.warehousemanagement.entity.Location;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends CrudRepository<Location, Long> {
    Optional<Location> findByAisle(String aisle);
}
