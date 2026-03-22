package com.example.warehousemanagement.client;

import com.example.warehousemanagement.dto.OutwardOrderDto;
import com.example.warehousemanagement.enums.OutwardOrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

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

    public List<OutwardOrderDto> getOutwardOrdersByStatus(String status) {
        String url = orderManagementUrl + "/api/outward-orders?status=" + status;
        try {
            OutwardOrderDto[] orders = restTemplate.getForObject(url, OutwardOrderDto[].class);
            return orders != null ? Arrays.asList(orders) : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public void updateOutwardOrderStatus(Long orderId, OutwardOrderStatus status) {
        String url = orderManagementUrl + "/api/outward-orders/" + orderId + "/status";
        UpdateStatusRequest request = new UpdateStatusRequest(status);
        restTemplate.put(url, request);
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

    private static class UpdateStatusRequest {
        private OutwardOrderStatus status;

        public UpdateStatusRequest() {
        }

        public UpdateStatusRequest(OutwardOrderStatus status) {
            this.status = status;
        }

        public OutwardOrderStatus getStatus() {
            return status;
        }

        public void setStatus(OutwardOrderStatus status) {
            this.status = status;
        }
    }
}
