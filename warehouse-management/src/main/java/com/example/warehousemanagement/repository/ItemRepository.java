package com.example.warehousemanagement.repository;

import com.example.warehousemanagement.entity.Item;
import com.example.warehousemanagement.enums.ItemStatus;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends CrudRepository<Item, Long> {
    List<Item> findByProductId(Long productId);
    List<Item> findByStatus(ItemStatus status);
}
