package com.example.productcatalog.controller;

import com.example.productcatalog.entity.ComboProduct;
import com.example.productcatalog.repository.ComboProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/combo-products")
public class ComboProductController {

    private final ComboProductRepository comboProductRepository;

    public ComboProductController(ComboProductRepository comboProductRepository) {
        this.comboProductRepository = comboProductRepository;
    }

    @PostMapping
    public ResponseEntity<Iterable<ComboProduct>> createComboProducts(@RequestBody List<ComboProduct> comboProducts) {
        for (ComboProduct cp : comboProducts) {
            cp.setId(null);
        }
        Iterable<ComboProduct> savedCombos = comboProductRepository.saveAll(comboProducts);
        return new ResponseEntity<>(savedCombos, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ComboProduct>> getAllComboProducts() {
        List<ComboProduct> combos = comboProductRepository.findAll();
        return ResponseEntity.ok(combos);
    }

    @GetMapping("/{comboProductId}")
    public ResponseEntity<List<ComboProduct>> getComboProductByComboId(@PathVariable("comboProductId") Long comboProductId) {
        List<ComboProduct> combos = comboProductRepository.findByComboProductId(comboProductId);
        if (combos.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(combos);
    }
}
