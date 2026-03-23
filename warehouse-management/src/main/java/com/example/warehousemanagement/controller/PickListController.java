package com.example.warehousemanagement.controller;

import com.example.warehousemanagement.entity.PickList;
import com.example.warehousemanagement.service.PickListService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pick-lists")
public class PickListController {

    private final PickListService pickListService;

    public PickListController(PickListService pickListService) {
        this.pickListService = pickListService;
    }

    @GetMapping
    public ResponseEntity<List<PickList>> getAllPickLists() {
        List<PickList> pickLists = pickListService.getAllPickLists();
        return ResponseEntity.ok(pickLists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PickList> getPickListById(@PathVariable("id") Long id) {
        Optional<PickList> pickList = pickListService.getPickListById(id);
        return pickList.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/pick")
    public ResponseEntity<PickList> pickItemForPickList(@PathVariable("id") Long id) {
        try {
            PickList updatedPickList = pickListService.pickItemForPickList(id);
            return ResponseEntity.ok(updatedPickList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
