package com.example.warehousemanagement.entity;

import com.example.commons.entity.BaseEntity;
import com.example.warehousemanagement.enums.BoxType;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("STORAGE_BOXES")
public class StorageBox extends BaseEntity {

    @Column("TYPE")
    private BoxType type;

    @Column("GATE_ENTRY_ID")
    private Long gateEntryId;

    @Column("LOCATION_ID")
    private Long locationId;

    @Column("WAREHOUSE_ID")
    private Long warehouseId;

    public StorageBox() {
    }

    public StorageBox(BoxType type, Long gateEntryId) {
        this.type = type;
        this.gateEntryId = gateEntryId;
    }

    public StorageBox(BoxType type, Long gateEntryId, Long locationId) {
        this.type = type;
        this.gateEntryId = gateEntryId;
        this.locationId = locationId;
    }

    public StorageBox(BoxType type, Long gateEntryId, Long locationId, Long warehouseId) {
        this.type = type;
        this.gateEntryId = gateEntryId;
        this.locationId = locationId;
        this.warehouseId = warehouseId;
    }

    public BoxType getType() {
        return type;
    }

    public void setType(BoxType type) {
        this.type = type;
    }

    public Long getGateEntryId() {
        return gateEntryId;
    }

    public void setGateEntryId(Long gateEntryId) {
        this.gateEntryId = gateEntryId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
