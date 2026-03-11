package com.example.ordermanagement.client;

import com.example.commons.enums.InventoryState;
import com.example.ordermanagement.dto.InventoryCreateRequest;
import com.example.ordermanagement.dto.InventoryMoveRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryServiceClient {

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryServiceClient(@Value("${inventory.service.url:http://localhost:8081}") String inventoryServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    public List<?> getInventoryByProductId(Long productId) {
        String url = inventoryServiceUrl + "/api/inventory/product/" + productId;
        return restTemplate.getForObject(url, List.class);
    }

    public void createInventory(Long productId, Integer quantity) {
        String url = inventoryServiceUrl + "/api/inventory";
        restTemplate.postForEntity(url, new InventoryCreateRequest(productId, quantity), Void.class);
    }

    public void moveInventory(Long productId, InventoryState fromState, InventoryState toState, Integer quantity) {
        String url = inventoryServiceUrl + "/api/inventory/move";
        restTemplate.postForEntity(url, new InventoryMoveRequest(productId, fromState, toState, quantity), Void.class);
    }

    public void updateReceivedInventory(Long productId, Integer quantity) {
        List<?> inventories = getInventoryByProductId(productId);
        boolean hasReceived = inventories != null && inventories.stream().anyMatch(item -> {
            if (item instanceof java.util.Map<?, ?> map) {
                Object state = map.get("state");
                return InventoryState.RECEIVED.name().equals(String.valueOf(state));
            }
            return false;
        });

        if (!hasReceived) {
            createInventory(productId, quantity);
        } else {
            moveInventory(productId, InventoryState.RECEIVED, InventoryState.RECEIVED, quantity);
        }
    }
}
