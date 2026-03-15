package com.example.warehousemanagement.controller;

import com.example.warehousemanagement.dto.AddItemToBoxRequest;
import com.example.warehousemanagement.dto.CompleteInwardRequest;
import com.example.warehousemanagement.dto.CycleCountRequest;
import com.example.warehousemanagement.dto.ItemCreateRequest;
import com.example.warehousemanagement.dto.PutAwayRequest;
import com.example.warehousemanagement.entity.BoxItem;
import com.example.warehousemanagement.entity.Item;
import com.example.warehousemanagement.service.ItemService;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody ItemCreateRequest request) {
        Item created = itemService.createItem(request);
        return ResponseEntity.created(URI.create("/api/items/" + created.getId()))
            .body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable("id") Long id) {
        Optional<Item> item = itemService.getItemById(id);
        return item.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/add-to-box")
    public ResponseEntity<BoxItem> addItemToBox(@RequestBody AddItemToBoxRequest request) {
        BoxItem boxItem = itemService.addItemToBox(request);
        return ResponseEntity.created(URI.create("/api/box-items/" + boxItem.getId()))
            .body(boxItem);
    }

    @GetMapping("/box/{boxId}")
    public ResponseEntity<List<BoxItem>> getItemsByBoxId(@PathVariable("boxId") Long boxId) {
        List<BoxItem> items = itemService.getItemsByBoxId(boxId);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/complete-inward")
    public ResponseEntity<Void> completeInward(@RequestBody CompleteInwardRequest request) {
        itemService.completeInwardForBox(request.getBoxId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/put-away")
    public ResponseEntity<Void> putAwayBox(@RequestBody PutAwayRequest request) {
        itemService.putAwayBox(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cycle-count")
    public ResponseEntity<Void> cycleCount(@RequestBody CycleCountRequest request) {
        itemService.cycleCount(request);
        return ResponseEntity.ok().build();
    }
}
