package com.example.warehousemanagement.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderManagementClient {

    private final RestTemplate restTemplate;
    private final String orderManagementUrl;

    public OrderManagementClient(@Value("${order.management.url:http://localhost:8082}") String orderManagementUrl) {
        this.restTemplate = new RestTemplate();
        this.orderManagementUrl = orderManagementUrl;
    }

    public void createGateEntry(Long inwardOrderId) {
        OrderManagementGateEntryCreateRequest request = new OrderManagementGateEntryCreateRequest(inwardOrderId);
        restTemplate.postForObject(orderManagementUrl + "/api/gate-entries", request, Object.class);
    }

    private static class OrderManagementGateEntryCreateRequest {
        private Long orderId;

        public OrderManagementGateEntryCreateRequest() {
        }

        public OrderManagementGateEntryCreateRequest(Long orderId) {
            this.orderId = orderId;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }
    }
}
