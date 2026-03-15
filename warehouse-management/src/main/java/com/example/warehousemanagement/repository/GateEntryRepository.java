package com.example.warehousemanagement.repository;

import com.example.warehousemanagement.entity.GateEntry;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GateEntryRepository extends CrudRepository<GateEntry, Long> {
    List<GateEntry> findByInwardOrderId(Long inwardOrderId);
}
