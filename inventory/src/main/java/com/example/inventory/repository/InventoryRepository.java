package com.example.inventory.repository;

import com.example.commons.enums.InventoryState;
import com.example.commons.enums.PackType;
import com.example.inventory.entity.Inventory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends CrudRepository<Inventory, Long> {
    List<Inventory> findByProductId(Long productId);
    Optional<Inventory> findByProductIdAndStateAndPackType(Long productId, InventoryState state, PackType packType);
    Optional<Inventory> findByProductIdAndPackType(Long productId, PackType packType);
    Optional<Inventory> findByProductIdAndStateAndPackTypeAndWarehouseId(Long productId, InventoryState state, PackType packType, Long warehouseId);
    Optional<Inventory> findByProductIdAndPackTypeAndWarehouseId(Long productId, PackType packType, Long warehouseId);
    List<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
}

