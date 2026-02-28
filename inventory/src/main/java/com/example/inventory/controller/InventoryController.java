package com.example.inventory.controller;

import com.example.commons.enums.InventoryState;
import com.example.commons.enums.PackType;
import com.example.inventory.entity.Inventory;
import com.example.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<Inventory> createInventory(@RequestBody CreateInventoryRequest request) {
        Inventory inventory = inventoryService.createInventory(request.getProductId(), request.getQuantity());
        return new ResponseEntity<>(inventory, HttpStatus.CREATED);
    }

    @PostMapping("/move")
    public ResponseEntity<Void> moveInventory(@RequestBody MoveInventoryRequest request) {
        inventoryService.moveInventory(request.getProductId(), request.getFromState(), request.getToState(), request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/convert")
    public ResponseEntity<Void> convertInventory(@RequestBody ConvertInventoryRequest request) {
        inventoryService.convertInventory(request.getSkuId(), request.getFromPackType(), request.getToPackType(), request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<java.util.List<Inventory>> getInventoryByProductId(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    public static class CreateInventoryRequest {
        private Long productId;
        private Integer quantity;

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

    public static class MoveInventoryRequest {
        private Long productId;
        private InventoryState fromState;
        private InventoryState toState;
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public InventoryState getFromState() {
            return fromState;
        }

        public void setFromState(InventoryState fromState) {
            this.fromState = fromState;
        }

        public InventoryState getToState() {
            return toState;
        }

        public void setToState(InventoryState toState) {
            this.toState = toState;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class ConvertInventoryRequest {
        private String skuId;
        private PackType fromPackType;
        private PackType toPackType;
        private Integer quantity;

        public String getSkuId() {
            return skuId;
        }

        public void setSkuId(String skuId) {
            this.skuId = skuId;
        }

        public PackType getFromPackType() {
            return fromPackType;
        }

        public void setFromPackType(PackType fromPackType) {
            this.fromPackType = fromPackType;
        }

        public PackType getToPackType() {
            return toPackType;
        }

        public void setToPackType(PackType toPackType) {
            this.toPackType = toPackType;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}

