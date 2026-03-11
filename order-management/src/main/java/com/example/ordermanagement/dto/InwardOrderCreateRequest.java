package com.example.ordermanagement.dto;

import com.example.ordermanagement.enums.Channel;

public class InwardOrderCreateRequest {
    private String channelOrderId;
    private Channel channel;

    public InwardOrderCreateRequest() {
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
}
