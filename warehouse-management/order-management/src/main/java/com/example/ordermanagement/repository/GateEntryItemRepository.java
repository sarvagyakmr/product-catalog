package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.GateEntryItem;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GateEntryItemRepository extends CrudRepository<GateEntryItem, Long> {
    List<GateEntryItem> findByGateEntryId(Long gateEntryId);
}
