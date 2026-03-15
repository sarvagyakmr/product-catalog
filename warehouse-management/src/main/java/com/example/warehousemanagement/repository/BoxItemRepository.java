package com.example.warehousemanagement.repository;

import com.example.warehousemanagement.entity.BoxItem;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoxItemRepository extends CrudRepository<BoxItem, Long> {
    List<BoxItem> findByBoxId(Long boxId);
    List<BoxItem> findByItemId(Long itemId);
}
