package com.example.warehousemanagement.repository;

import com.example.warehousemanagement.entity.OutwardOrderItem;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutwardOrderItemRepository extends CrudRepository<OutwardOrderItem, Long> {
    List<OutwardOrderItem> findByOrderId(Long orderId);
}
