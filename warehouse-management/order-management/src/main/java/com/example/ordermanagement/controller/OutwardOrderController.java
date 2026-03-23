package com.example.ordermanagement.controller;

import com.example.ordermanagement.dto.OutwardOrderCreateRequest;
import com.example.ordermanagement.entity.OutwardOrder;
import com.example.ordermanagement.entity.OutwardOrderItem;
import com.example.ordermanagement.enums.OutwardOrderStatus;
import com.example.ordermanagement.service.OutwardOrderService;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outward-orders")
public class OutwardOrderController {

    private final OutwardOrderService outwardOrderService;

    public OutwardOrderController(OutwardOrderService outwardOrderService) {
        this.outwardOrderService = outwardOrderService;
    }

    @PostMapping
    public ResponseEntity<OutwardOrder> createOutwardOrder(@RequestBody OutwardOrderCreateRequest request) {
        OutwardOrder created = outwardOrderService.createOutwardOrder(request);
        return ResponseEntity.created(URI.create("/api/outward-orders/" + created.getId()))
            .body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutwardOrder> getOutwardOrderById(@PathVariable("id") Long id) {
        Optional<OutwardOrder> order = outwardOrderService.getOutwardOrderById(id);
        return order.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<OutwardOrderItem>> getOrderItems(@PathVariable("id") Long id) {
        List<OutwardOrderItem> items = outwardOrderService.getItemsByOrderId(id);
        return ResponseEntity.ok(items);
    }

    @GetMapping
    public ResponseEntity<List<OutwardOrderResponse>> getOutwardOrdersByStatus(@RequestParam("status") OutwardOrderStatus status) {
        Iterable<OutwardOrder> orders = outwardOrderService.getOrdersByStatus(status);
        List<OutwardOrderResponse> responseList = StreamSupport.stream(orders.spliterator(), false)
            .map(order -> {
                OutwardOrderResponse resp = new OutwardOrderResponse();
                resp.setId(order.getId());
                resp.setChannelOrderId(order.getChannelOrderId());
                resp.setChannel(order.getChannel());
                resp.setWarehouseId(order.getWarehouseId());
                resp.setStatus(order.getStatus());
                resp.setTimestamp(order.getTimestamp());
                
                // Fetch items
                List<OutwardOrderItem> items = outwardOrderService.getItemsByOrderId(order.getId());
                resp.setItems(items.stream().map(item -> {
                    OutwardOrderItemResponse itemResp = new OutwardOrderItemResponse();
                    itemResp.setId(item.getId());
                    itemResp.setProductId(item.getProductId());
                    itemResp.setOrderedQuantity(item.getOrderedQuantity());
                    itemResp.setAllocatedQuantity(item.getAllocatedQuantity());
                    return itemResp;
                }).collect(Collectors.toList()));
                
                return resp;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OutwardOrder> updateOutwardOrderStatus(@PathVariable("id") Long id, @RequestBody UpdateStatusRequest request) {
        // If status is CANCELLED, use the cancel order flow which handles inventory updates
        if (request.getStatus() == OutwardOrderStatus.CANCELLED) {
            try {
                OutwardOrder cancelledOrder = outwardOrderService.cancelOrder(id);
                return ResponseEntity.ok(cancelledOrder);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.notFound().build();
            } catch (IllegalStateException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        // For other status updates, just update the status directly
        Optional<OutwardOrder> orderOpt = outwardOrderService.getOutwardOrderById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        OutwardOrder order = orderOpt.get();
        order.setStatus(request.getStatus());
        outwardOrderService.save(order);
        return ResponseEntity.ok(order);
    }

    public static class UpdateStatusRequest {
        private OutwardOrderStatus status;

        public OutwardOrderStatus getStatus() {
            return status;
        }

        public void setStatus(OutwardOrderStatus status) {
            this.status = status;
        }
    }

    public static class OutwardOrderResponse {
        private Long id;
        private String channelOrderId;
        private com.example.ordermanagement.enums.Channel channel;
        private Long warehouseId;
        private com.example.ordermanagement.enums.OutwardOrderStatus status;
        private java.time.OffsetDateTime timestamp;
        private List<OutwardOrderItemResponse> items;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getChannelOrderId() { return channelOrderId; }
        public void setChannelOrderId(String channelOrderId) { this.channelOrderId = channelOrderId; }
        public com.example.ordermanagement.enums.Channel getChannel() { return channel; }
        public void setChannel(com.example.ordermanagement.enums.Channel channel) { this.channel = channel; }
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
        public com.example.ordermanagement.enums.OutwardOrderStatus getStatus() { return status; }
        public void setStatus(com.example.ordermanagement.enums.OutwardOrderStatus status) { this.status = status; }
        public java.time.OffsetDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.OffsetDateTime timestamp) { this.timestamp = timestamp; }
        public List<OutwardOrderItemResponse> getItems() { return items; }
        public void setItems(List<OutwardOrderItemResponse> items) { this.items = items; }
    }

    public static class OutwardOrderItemResponse {
        private Long id;
        private Long productId;
        private Integer orderedQuantity;
        private Integer allocatedQuantity;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getOrderedQuantity() { return orderedQuantity; }
        public void setOrderedQuantity(Integer orderedQuantity) { this.orderedQuantity = orderedQuantity; }
        public Integer getAllocatedQuantity() { return allocatedQuantity; }
        public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }
    }
}
