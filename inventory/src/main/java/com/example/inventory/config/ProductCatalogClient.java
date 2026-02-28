package com.example.inventory.config;

import com.example.inventory.dto.ComboProductDto;
import com.example.inventory.dto.PackConversionDto;
import com.example.inventory.dto.ProductDto;
import com.example.inventory.enums.PackType;
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

    public Optional<ProductDto> getProductBySkuIdAndPackType(String skuId, PackType packType) {
        try {
            String url = String.format("%s/api/products/sku/%s/pack-type/%s", productCatalogUrl, skuId, packType);
            return Optional.ofNullable(restTemplate.getForObject(url, ProductDto.class));
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

    public Optional<PackConversionDto> getPackConversion(String skuId, PackConversionDto.PackConversionQuery query) {
        try {
            String url = String.format("%s/api/pack-conversions/%s?fromPackType=%s&toPackType=%s",
                    productCatalogUrl, skuId, query.getFromPackType(), query.getToPackType());
            return Optional.ofNullable(restTemplate.getForObject(url, PackConversionDto.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch pack conversion from catalog", e);
        }
    }
}


