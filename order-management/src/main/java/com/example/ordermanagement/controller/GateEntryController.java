package com.example.ordermanagement.controller;

import com.example.ordermanagement.dto.GateEntryCreateRequest;
import com.example.ordermanagement.entity.GateEntry;
import com.example.ordermanagement.service.GateEntryService;
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
@RequestMapping("/api/gate-entries")
public class GateEntryController {

    private final GateEntryService gateEntryService;

    public GateEntryController(GateEntryService gateEntryService) {
        this.gateEntryService = gateEntryService;
    }

    @PostMapping
    public ResponseEntity<GateEntry> createGateEntry(@RequestBody GateEntryCreateRequest request) {
        GateEntry created = gateEntryService.createGateEntry(request);
        return ResponseEntity.created(URI.create("/api/gate-entries/" + created.getId()))
            .body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GateEntry> getGateEntryById(@PathVariable("id") Long id) {
        Optional<GateEntry> gateEntry = gateEntryService.getGateEntryById(id);
        return gateEntry.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
