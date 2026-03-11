package com.example.ordermanagement.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("GATE_ENTRY_ITEMS")
public class GateEntryItem {

    @Id
    @Column("ID")
    private Long id;

    @Column("GATE_ENTRY_ID")
    private Long gateEntryId;

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("QUANTITY")
    private Integer quantity;

    public GateEntryItem() {
    }

    public GateEntryItem(Long gateEntryId, Long productId, Integer quantity) {
        this.gateEntryId = gateEntryId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGateEntryId() {
        return gateEntryId;
    }

    public void setGateEntryId(Long gateEntryId) {
        this.gateEntryId = gateEntryId;
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
}
