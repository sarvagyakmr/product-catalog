package com.example.ordermanagement.entity;

import com.example.ordermanagement.enums.Channel;
import com.example.ordermanagement.enums.InwardOrderStatus;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("INWARD_ORDERS")
public class InwardOrder {

    @Id
    @Column("ID")
    private Long id;

    @Column("CHANNEL_ORDER_ID")
    private String channelOrderId;

    @Column("CHANNEL")
    private Channel channel;

    @Column("TIMESTAMP")
    private OffsetDateTime timestamp;

    @Column("STATUS")
    private InwardOrderStatus status;

    public InwardOrder() {
    }

    public InwardOrder(String channelOrderId, Channel channel, OffsetDateTime timestamp, InwardOrderStatus status) {
        this.channelOrderId = channelOrderId;
        this.channel = channel;
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

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public InwardOrderStatus getStatus() {
        return status;
    }

    public void setStatus(InwardOrderStatus status) {
        this.status = status;
    }
}
