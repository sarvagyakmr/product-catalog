package com.example.warehousemanagement.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("GATE_ENTRIES")
public class GateEntry {

    @Id
    @Column("ID")
    private Long id;

    @Column("INWARD_ORDER_ID")
    private Long inwardOrderId;

    public GateEntry() {
    }

    public GateEntry(Long inwardOrderId) {
        this.inwardOrderId = inwardOrderId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInwardOrderId() {
        return inwardOrderId;
    }

    public void setInwardOrderId(Long inwardOrderId) {
        this.inwardOrderId = inwardOrderId;
    }
}
