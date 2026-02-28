package com.example.inventory.entity;

import com.example.commons.enums.InventoryState;
import com.example.commons.enums.PackType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("INVENTORY")
public class Inventory {

    @Id
    @Column("ID")
    private Long id;

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("PACK_TYPE")
    private PackType packType;

    @Column("QUANTITY")
    private Integer quantity;

    @Column("STATE")
    private InventoryState state;

    public Inventory() {
    }

    public Inventory(Long productId, PackType packType, Integer quantity, InventoryState state) {
        this.productId = productId;
        this.packType = packType;
        this.quantity = quantity;
        this.state = state;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public PackType getPackType() {
        return packType;
    }

    public void setPackType(PackType packType) {
        this.packType = packType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public InventoryState getState() {
        return state;
    }

    public void setState(InventoryState state) {
        this.state = state;
    }
}
