package com.example.productcatalog.controller;

import com.example.commons.enums.PackType;
import com.example.productcatalog.entity.Product;
import com.example.productcatalog.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        product.setId(null);
        Product savedProduct = productRepository.save(product);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(value = "skuId", required = false) String skuId,
            @RequestParam(value = "packType", required = false) PackType packType) {
        if (skuId == null) {
            List<Product> products = productRepository.findAll();
            return ResponseEntity.ok(products);
        }
        List<Product> products = productRepository.findBySkuId(skuId);
        if (products.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (packType != null) {
            return products.stream()
                    .filter(product -> packType == product.getPackType())
                    .findFirst()
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        return ResponseEntity.ok(products);
    }
}
