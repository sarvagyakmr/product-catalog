package com.example.ordermanagement.client;

import com.example.ordermanagement.dto.ProductResponse;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductCatalogServiceClient {

    private final RestTemplate restTemplate;
    private final String productCatalogUrl;

    public ProductCatalogServiceClient(@Value("${product.catalog.url:http://localhost:8080}") String productCatalogUrl) {
        this.restTemplate = new RestTemplate();
        this.productCatalogUrl = productCatalogUrl;
    }

    public Optional<ProductResponse> getProduct(Long productId) {
        try {
            ProductResponse response = restTemplate.getForObject(productCatalogUrl + "/api/products/" + productId, ProductResponse.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
