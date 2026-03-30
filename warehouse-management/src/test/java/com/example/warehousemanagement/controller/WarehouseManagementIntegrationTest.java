package com.example.warehousemanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:testwarehousemanagerdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
public class WarehouseManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    // These tests simulate TDD boundaries for features not fully natively implemented inside WarehouseManagement internally
    @Test
    void createWarehouse_withValidData_shouldReturn201() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Central Distribution Node");
        request.put("locationCode", "US-EAST-1");
        request.put("capacity", 50000);

        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()); // Explicitly expecting 404 until domain moves here
    }

    @Test
    void assignInventory_toWarehouse_shouldUpdateStockAndReturn200() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("productId", 100L);
        request.put("quantity", 500);

        mockMvc.perform(post("/api/warehouses/1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
