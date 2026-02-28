package com.example.inventory.repository;

import com.example.inventory.entity.Inventory;
import com.example.inventory.enums.InventoryState;
import com.example.inventory.enums.PackType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends CrudRepository<Inventory, Long> {
    List<Inventory> findByProductId(Long productId);
    Optional<Inventory> findByProductIdAndStateAndPackType(Long productId, InventoryState state, PackType packType);
    Optional<Inventory> findByProductIdAndPackType(Long productId, PackType packType);
}

