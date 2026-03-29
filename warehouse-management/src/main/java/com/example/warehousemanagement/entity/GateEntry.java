package com.example.warehousemanagement.entity;

import com.example.commons.entity.BaseEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("GATE_ENTRIES")
public class GateEntry extends BaseEntity {

    @Column("INWARD_ORDER_ID")
    private Long inwardOrderId;

    public GateEntry() {
    }

    public GateEntry(Long inwardOrderId) {
        this.inwardOrderId = inwardOrderId;
    }

    public Long getInwardOrderId() {
        return inwardOrderId;
    }

    public void setInwardOrderId(Long inwardOrderId) {
        this.inwardOrderId = inwardOrderId;
    }
}
