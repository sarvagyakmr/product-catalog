package com.example.warehousemanagement.repository;

import com.example.warehousemanagement.entity.OutwardOrder;
import com.example.warehousemanagement.enums.OutwardOrderStatus;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutwardOrderRepository extends CrudRepository<OutwardOrder, Long> {
    Optional<OutwardOrder> findByOrderManagementId(Long orderManagementId);
    Iterable<OutwardOrder> findByStatus(OutwardOrderStatus status);
}
