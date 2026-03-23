package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.InwardOrderItem;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InwardOrderItemRepository extends CrudRepository<InwardOrderItem, Long> {
    List<InwardOrderItem> findByOrderId(Long orderId);
}
