package com.example.commons.repository;

import com.example.commons.entity.Inventory;
import com.example.commons.enums.InventoryState;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends CrudRepository<Inventory, Long> {
    Optional<Inventory> findByProductIdAndState(Long productId, InventoryState state);
}
