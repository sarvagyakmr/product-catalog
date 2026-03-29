package com.example.warehousemanagement.entity;

import com.example.commons.entity.BaseEntity;
import com.example.commons.enums.OutwardOrderStatus;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("WM_OUTWARD_ORDERS")
public class OutwardOrder extends BaseEntity {

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

    public OutwardOrder() {
    }

    public OutwardOrder(Long orderManagementId, String channelOrderId, String channel, Long warehouseId, OutwardOrderStatus status) {
        this.orderManagementId = orderManagementId;
        this.channelOrderId = channelOrderId;
        this.channel = channel;
        this.warehouseId = warehouseId;
        this.status = status;
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
}
