package com.example.ordermanagement.dto;

import com.example.ordermanagement.enums.WarehouseType;

public class WarehouseCreateRequest {
    private String name;
    private String code;
    private WarehouseType type;
    private String address;

    public WarehouseCreateRequest() {
    }

    public WarehouseCreateRequest(String name, String code, WarehouseType type, String address) {
        this.name = name;
        this.code = code;
        this.type = type;
        this.address = address;
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
