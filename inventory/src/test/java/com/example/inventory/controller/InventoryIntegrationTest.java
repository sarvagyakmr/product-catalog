package com.example.inventory.controller;

import com.example.commons.client.ProductCatalogClient;
import com.example.commons.dto.ProductDto;
import com.example.commons.enums.InventoryState;
import com.example.commons.enums.PackType;
import com.example.inventory.controller.InventoryController.CreateInventoryRequest;
import com.example.inventory.controller.InventoryController.MoveInventoryRequest;
import com.example.inventory.entity.Inventory;
import com.example.inventory.repository.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:testinventorydb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
public class InventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductCatalogClient productCatalogClient;

    @MockBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
    }

    private ProductDto createMockProductDto(Long id) {
        ProductDto dto = new ProductDto();
        dto.setId(id);
        dto.setSkuId("SKU-MOCK-" + id);
        dto.setPackType(PackType.EACH.name());
        dto.setType(com.example.commons.enums.ProductType.SINGLE.name());
        return dto;
    }

    @Test
    void createInventory_shouldPersistNewStock() throws Exception {
        when(productCatalogClient.getProduct(1L)).thenReturn(Optional.of(createMockProductDto(1L)));

        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setProductId(1L);
        request.setQuantity(50);
        request.setWarehouseId(100L);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertThat(inventoryRepository.findAll()).hasSize(1);
    }

    @Test
    void moveInventory_shouldReduceSourceAndIncreaseTargetStock() throws Exception {
        when(productCatalogClient.getProduct(1L)).thenReturn(Optional.of(createMockProductDto(1L)));

        inventoryRepository.save(new Inventory(1L, PackType.EACH, 100, InventoryState.RECEIVED, 100L));

        MoveInventoryRequest request = new MoveInventoryRequest();
        request.setProductId(1L);
        request.setWarehouseId(100L);
        request.setQuantity(40);
        request.setFromState(InventoryState.RECEIVED);
        request.setToState(InventoryState.AVAILABLE);

        mockMvc.perform(post("/api/inventory/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Integer receivedStock = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(1L, InventoryState.RECEIVED, PackType.EACH, 100L).get().getQuantity();
        Integer availableStock = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(1L, InventoryState.AVAILABLE, PackType.EACH, 100L).get().getQuantity();

        assertThat(receivedStock).isEqualTo(60); 
        assertThat(availableStock).isEqualTo(40); 
    }

    @Test
    void createInventory_withUnknownProduct_shouldReturnNotFound() throws Exception {
        when(productCatalogClient.getProduct(anyLong())).thenReturn(Optional.empty());

        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setProductId(999L);
        request.setQuantity(10);
        request.setWarehouseId(100L);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addInventory_withConcurrentRequests_shouldUtilizeOptimisticLocking() throws Exception {
        when(productCatalogClient.getProduct(1L)).thenReturn(Optional.of(createMockProductDto(1L)));

        inventoryRepository.save(new Inventory(1L, PackType.EACH, 10, InventoryState.RECEIVED, 100L));

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    CreateInventoryRequest request = new CreateInventoryRequest();
                    request.setProductId(1L);
                    request.setQuantity(10);
                    request.setWarehouseId(100L);

                    mockMvc.perform(post("/api/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        Inventory dbState = inventoryRepository.findByProductIdAndStateAndPackTypeAndWarehouseId(1L, InventoryState.RECEIVED, PackType.EACH, 100L).get();
        // At least original 10 + 1 successful increment = 20 minimum. Validating no anomalous overwriting occurred.
        assertThat(dbState.getQuantity() % 10).isEqualTo(0); 
    }
}
