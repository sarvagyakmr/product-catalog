package com.example.ordermanagement.entity;

import com.example.ordermanagement.enums.WarehouseType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("WAREHOUSES")
public class Warehouse {

    @Id
    @Column("ID")
    private Long id;

    @Column("NAME")
    private String name;

    @Column("CODE")
    private String code;

    @Column("TYPE")
    private WarehouseType type;

    @Column("ADDRESS")
    private String address;

    public Warehouse() {
    }

    public Warehouse(String name, String code, WarehouseType type, String address) {
        this.name = name;
        this.code = code;
        this.type = type;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public WarehouseType getType() {
        return type;
    }

    public void setType(WarehouseType type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
