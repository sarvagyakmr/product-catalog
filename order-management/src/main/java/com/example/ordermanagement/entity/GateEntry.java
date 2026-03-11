package com.example.ordermanagement.entity;

import com.example.ordermanagement.enums.GateEntryStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("GATE_ENTRIES")
public class GateEntry {

    @Id
    @Column("ID")
    private Long id;

    @Column("ORDER_ID")
    private Long orderId;

    @Column("STATUS")
    private GateEntryStatus status;

    public GateEntry() {
    }

    public GateEntry(Long orderId, GateEntryStatus status) {
        this.orderId = orderId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public GateEntryStatus getStatus() {
        return status;
    }

    public void setStatus(GateEntryStatus status) {
        this.status = status;
    }
}
