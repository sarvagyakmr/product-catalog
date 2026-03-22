package com.example.warehousemanagement.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("LOCATIONS")
public class Location {

    @Id
    @Column("ID")
    private Long id;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
