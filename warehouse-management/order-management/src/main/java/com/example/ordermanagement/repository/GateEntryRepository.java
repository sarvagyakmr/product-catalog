package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.GateEntry;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GateEntryRepository extends CrudRepository<GateEntry, Long> {
    List<GateEntry> findByOrderId(Long orderId);
}
