package com.example.ordermanagement.entity;

import com.example.ordermanagement.enums.Channel;
import com.example.ordermanagement.enums.OutwardOrderStatus;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("OUTWARD_ORDERS")
public class OutwardOrder {

    @Id
    @Column("ID")
    private Long id;

    @Column("CHANNEL_ORDER_ID")
    private String channelOrderId;

    @Column("CHANNEL")
    private Channel channel;

    @Column("WAREHOUSE_ID")
    private Long warehouseId;

    @Column("TIMESTAMP")
    private OffsetDateTime timestamp;

    @Column("STATUS")
    private OutwardOrderStatus status;

    public OutwardOrder() {
    }

    public OutwardOrder(String channelOrderId, Channel channel, Long warehouseId, OffsetDateTime timestamp, OutwardOrderStatus status) {
        this.channelOrderId = channelOrderId;
        this.channel = channel;
        this.warehouseId = warehouseId;
        this.timestamp = timestamp;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelOrderId() {
        return channelOrderId;
    }

    public void setChannelOrderId(String channelOrderId) {
        this.channelOrderId = channelOrderId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public OutwardOrderStatus getStatus() {
        return status;
    }

    public void setStatus(OutwardOrderStatus status) {
        this.status = status;
    }
}
