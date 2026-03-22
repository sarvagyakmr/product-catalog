package com.example.warehousemanagement.controller;

import com.example.warehousemanagement.entity.OutwardOrder;
import com.example.warehousemanagement.entity.OutwardOrderItem;
import com.example.warehousemanagement.service.OutwardOrderService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outward-orders")
public class OutwardOrderController {

    private final OutwardOrderService outwardOrderService;

    public OutwardOrderController(OutwardOrderService outwardOrderService) {
        this.outwardOrderService = outwardOrderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutwardOrder> getOutwardOrderById(@PathVariable("id") Long id) {
        Optional<OutwardOrder> order = outwardOrderService.getOutwardOrderById(id);
        return order.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<OutwardOrderItem>> getOutwardOrderItems(@PathVariable("id") Long id) {
        List<OutwardOrderItem> items = outwardOrderService.getItemsByOrderId(id);
        return ResponseEntity.ok(items);
    }
}
