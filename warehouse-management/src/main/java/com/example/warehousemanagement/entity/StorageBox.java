package com.example.warehousemanagement.entity;

import com.example.warehousemanagement.enums.BoxType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("STORAGE_BOXES")
public class StorageBox {

    @Id
    @Column("ID")
    private Long id;

    @Column("TYPE")
    private BoxType type;

    @Column("GATE_ENTRY_ID")
    private Long gateEntryId;

    @Column("LOCATION_ID")
    private Long locationId;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
