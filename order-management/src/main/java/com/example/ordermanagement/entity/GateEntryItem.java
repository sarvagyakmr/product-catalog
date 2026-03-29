package com.example.ordermanagement.entity;

import com.example.commons.entity.BaseEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("GATE_ENTRY_ITEMS")
public class GateEntryItem extends BaseEntity {

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
