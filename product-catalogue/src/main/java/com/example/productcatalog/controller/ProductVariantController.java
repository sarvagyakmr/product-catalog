package com.example.productcatalog.controller;

import com.example.productcatalog.entity.ProductVariant;
import com.example.productcatalog.repository.ProductVariantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/product-variants")
public class ProductVariantController {

    private final ProductVariantRepository productVariantRepository;

    public ProductVariantController(ProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    @PostMapping
    public ResponseEntity<ProductVariant> createProductVariant(@RequestBody ProductVariant productVariant) {
        productVariant.setId(null);
        ProductVariant savedVariant = productVariantRepository.save(productVariant);
        return new ResponseEntity<>(savedVariant, HttpStatus.CREATED);
    }


    @GetMapping
    public ResponseEntity<List<ProductVariant>> getAllProductVariants() {
        List<ProductVariant> variants = productVariantRepository.findAll();
        return ResponseEntity.ok(variants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductVariant> getProductVariantById(@PathVariable("id") Long id) {
        Optional<ProductVariant> variant = productVariantRepository.findById(id);
        return variant.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductVariant>> getProductVariantsByProductId(@PathVariable("productId") Long productId) {
        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        return ResponseEntity.ok(variants);
    }
}
