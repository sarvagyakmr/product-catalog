package com.example.warehousemanagement.entity;

import com.example.commons.entity.BaseEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("LOCATIONS")
public class Location extends BaseEntity {

    @Column("AISLE")
    private String aisle;

    @Column("DISPLAY_NAME")
    private String displayName;

    @Column("WAREHOUSE_ID")
    private Long warehouseId;

    public Location() {
    }

    public Location(String aisle, String displayName) {
        this.aisle = aisle;
        this.displayName = displayName;
    }

    public Location(String aisle, String displayName, Long warehouseId) {
        this.aisle = aisle;
        this.displayName = displayName;
        this.warehouseId = warehouseId;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
