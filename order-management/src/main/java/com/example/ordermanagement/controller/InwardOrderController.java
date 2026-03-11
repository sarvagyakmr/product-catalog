package com.example.ordermanagement.controller;

import com.example.ordermanagement.dto.InwardOrderCreateRequest;
import com.example.ordermanagement.entity.InwardOrder;
import com.example.ordermanagement.service.InwardOrderService;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inward-orders")
public class InwardOrderController {

    private final InwardOrderService inwardOrderService;

    public InwardOrderController(InwardOrderService inwardOrderService) {
        this.inwardOrderService = inwardOrderService;
    }

    @PostMapping
    public ResponseEntity<InwardOrder> createInwardOrder(@RequestBody InwardOrderCreateRequest request) {
        InwardOrder created = inwardOrderService.createInwardOrder(request);
        return ResponseEntity.created(URI.create("/api/inward-orders/" + created.getId()))
            .body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InwardOrder> getInwardOrderById(@PathVariable("id") Long id) {
        Optional<InwardOrder> inwardOrder = inwardOrderService.getInwardOrderById(id);
        return inwardOrder.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
