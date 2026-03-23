package com.example.ordermanagement.dto;

import com.example.ordermanagement.enums.Channel;
import java.util.List;

public class OutwardOrderCreateRequest {
    private String channelOrderId;
    private Channel channel;
    private Long warehouseId;
    private List<OutwardOrderItemRequest> items;

    public OutwardOrderCreateRequest() {
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

    public List<OutwardOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OutwardOrderItemRequest> items) {
        this.items = items;
    }

    public static class OutwardOrderItemRequest {
        private Long productId;
        private Integer quantity;

        public OutwardOrderItemRequest() {
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
}
