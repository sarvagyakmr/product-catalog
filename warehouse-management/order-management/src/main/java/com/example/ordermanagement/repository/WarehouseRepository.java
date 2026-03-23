package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.Warehouse;
import com.example.ordermanagement.enums.WarehouseType;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends CrudRepository<Warehouse, Long> {
    Optional<Warehouse> findByCode(String code);
    Iterable<Warehouse> findByType(WarehouseType type);
}
