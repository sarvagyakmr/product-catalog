package com.example.ordermanagement.entity;

import com.example.commons.entity.BaseEntity;
import com.example.ordermanagement.enums.GateEntryStatus;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("GATE_ENTRIES")
public class GateEntry extends BaseEntity {

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
