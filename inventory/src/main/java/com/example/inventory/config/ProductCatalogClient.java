package com.example.inventory.config;

import com.example.inventory.dto.ComboProductDto;
import com.example.inventory.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class ProductCatalogClient {

    private final RestTemplate restTemplate;
    private final String productCatalogUrl;

    public ProductCatalogClient(@Value("${product.catalog.url:http://localhost:8080}") String productCatalogUrl) {
        this.restTemplate = new RestTemplate();
        this.productCatalogUrl = productCatalogUrl;
    }

    public Optional<ProductDto> getProduct(Long productId) {
        try {
            return Optional.ofNullable(restTemplate.getForObject(productCatalogUrl + "/api/products/" + productId, ProductDto.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch product from catalog", e);
        }
    }

    public List<ComboProductDto> getComboProduct(Long comboId) {
        try {
            ComboProductDto[] response = restTemplate.getForObject(productCatalogUrl + "/api/combo-products/" + comboId, ComboProductDto[].class);
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound e) {
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch combo product from catalog", e);
        }
    }
}


