package com.example.warehousemanagement.repository;

import com.example.warehousemanagement.entity.PickList;
import com.example.warehousemanagement.enums.PickListStatus;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PickListRepository extends CrudRepository<PickList, Long> {
    List<PickList> findByOrderId(Long orderId);
    List<PickList> findByStatus(PickListStatus status);
    List<PickList> findByOrderIdAndProductId(Long orderId, Long productId);
}
