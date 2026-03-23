package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.OutwardOrderItem;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutwardOrderItemRepository extends CrudRepository<OutwardOrderItem, Long> {
    List<OutwardOrderItem> findByOrderId(Long orderId);
}
