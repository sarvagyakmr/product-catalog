package com.example.commons.entity;

import com.example.commons.enums.InventoryState;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("INVENTORY")
public class Inventory extends BaseEntity {

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("QUANTITY")
    private Integer quantity;

    @Column("STATE")
    private InventoryState state;

    public Inventory() {
    }

    public Inventory(Long productId, Integer quantity, InventoryState state) {
        this.productId = productId;
        this.quantity = quantity;
        this.state = state;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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
