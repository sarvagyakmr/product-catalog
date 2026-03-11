package com.example.ordermanagement.controller;

import com.example.ordermanagement.dto.GateEntryItemCreateRequest;
import com.example.ordermanagement.entity.GateEntryItem;
import com.example.ordermanagement.service.GateEntryItemService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gate-entries/{gateEntryId}/items")
public class GateEntryItemController {

    private final GateEntryItemService gateEntryItemService;

    public GateEntryItemController(GateEntryItemService gateEntryItemService) {
        this.gateEntryItemService = gateEntryItemService;
    }

    @PostMapping
    public ResponseEntity<GateEntryItem> addGateEntryItem(
            @PathVariable("gateEntryId") Long gateEntryId,
            @RequestBody GateEntryItemCreateRequest request) {
        GateEntryItem created = gateEntryItemService.addGateEntryItem(gateEntryId, request);
        return ResponseEntity.created(URI.create("/api/gate-entries/" + gateEntryId + "/items/" + created.getId()))
            .body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GateEntryItem> getGateEntryItemById(
            @PathVariable("gateEntryId") Long gateEntryId,
            @PathVariable("id") Long id) {
        return gateEntryItemService.getGateEntryItemById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
