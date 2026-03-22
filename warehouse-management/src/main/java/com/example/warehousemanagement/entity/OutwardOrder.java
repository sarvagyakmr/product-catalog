package com.example.warehousemanagement.entity;

import com.example.warehousemanagement.enums.OutwardOrderStatus;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("WM_OUTWARD_ORDERS")
public class OutwardOrder {

    @Id
    @Column("ID")
    private Long id;

    @Column("ORDER_MANAGEMENT_ID")
    private Long orderManagementId;

    @Column("CHANNEL_ORDER_ID")
    private String channelOrderId;

    @Column("CHANNEL")
    private String channel;

    @Column("WAREHOUSE_ID")
    private Long warehouseId;

    @Column("STATUS")
    private OutwardOrderStatus status;

    @Column("CREATED_AT")
    private OffsetDateTime createdAt;

    public OutwardOrder() {
    }

    public OutwardOrder(Long orderManagementId, String channelOrderId, String channel, Long warehouseId, OutwardOrderStatus status) {
        this.orderManagementId = orderManagementId;
        this.channelOrderId = channelOrderId;
        this.channel = channel;
        this.warehouseId = warehouseId;
        this.status = status;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderManagementId() {
        return orderManagementId;
    }

    public void setOrderManagementId(Long orderManagementId) {
        this.orderManagementId = orderManagementId;
    }

    public String getChannelOrderId() {
        return channelOrderId;
    }

    public void setChannelOrderId(String channelOrderId) {
        this.channelOrderId = channelOrderId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public OutwardOrderStatus getStatus() {
        return status;
    }

    public void setStatus(OutwardOrderStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
