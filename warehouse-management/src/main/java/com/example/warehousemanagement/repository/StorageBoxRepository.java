package com.example.warehousemanagement.repository;

import com.example.warehousemanagement.entity.StorageBox;
import com.example.warehousemanagement.enums.BoxType;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageBoxRepository extends CrudRepository<StorageBox, Long> {
    List<StorageBox> findByGateEntryId(Long gateEntryId);
    List<StorageBox> findByType(BoxType type);
    List<StorageBox> findByLocationId(Long locationId);
}
