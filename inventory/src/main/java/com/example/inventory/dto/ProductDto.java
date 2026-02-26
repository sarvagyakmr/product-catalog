package com.example.inventory.dto;

public class ProductDto {
    private Long id;
    private String type; // SINGLE, COMBO

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
