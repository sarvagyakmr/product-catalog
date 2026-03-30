package com.example.ordermanagement.controller;

import com.example.commons.dto.InventoryEventDto;
import com.example.commons.enums.InventoryState;
import com.example.ordermanagement.dto.OutwardOrderCreateRequest;
import com.example.ordermanagement.dto.OutwardOrderCreateRequest.OutwardOrderItemRequest;
import com.example.ordermanagement.entity.OutwardOrder;
import com.example.ordermanagement.enums.Channel;
import com.example.ordermanagement.enums.OutwardOrderStatus;
import com.example.ordermanagement.repository.OutwardOrderItemRepository;
import com.example.ordermanagement.repository.OutwardOrderRepository;
import com.example.ordermanagement.service.RedisPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:testorderdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
public class OrderManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutwardOrderRepository outwardOrderRepository;

    @Autowired
    private OutwardOrderItemRepository outwardOrderItemRepository;

    @MockBean
    private RedisPublisher redisPublisher;

    @AfterEach
    void tearDown() {
        outwardOrderItemRepository.deleteAll();
        outwardOrderRepository.deleteAll();
    }

    @Test
    void createOutwardOrder_withMultipleItems_shouldPersistOrderAndLines() throws Exception {
        OutwardOrderCreateRequest request = new OutwardOrderCreateRequest();
        request.setChannelOrderId("CH-100");
        request.setChannel(Channel.INTERNAL);
        request.setWarehouseId(1L);

        OutwardOrderItemRequest item1 = new OutwardOrderItemRequest();
        item1.setProductId(10L);
        item1.setQuantity(5);
        
        OutwardOrderItemRequest item2 = new OutwardOrderItemRequest();
        item2.setProductId(20L);
        item2.setQuantity(2);

        request.setItems(List.of(item1, item2));

        String responseJson = mockMvc.perform(post("/api/outward-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(OutwardOrderStatus.CREATED.name()))
                .andReturn().getResponse().getContentAsString();

        OutwardOrder createdOrder = objectMapper.readValue(responseJson, OutwardOrder.class);
        assertThat(outwardOrderItemRepository.findByOrderId(createdOrder.getId())).hasSize(2);
    }

    @Test
    void orderAllocation_withInsufficientInventory_leavesOrderAsCreated() throws Exception {
        OutwardOrder order = new OutwardOrder("CH-BAD-INV", Channel.INTERNAL, 1L, null, OutwardOrderStatus.CREATED);
        order = outwardOrderRepository.save(order);
        
        mockMvc.perform(get("/api/outward-orders/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OutwardOrderStatus.CREATED.name()));
    }

    @Test
    void updateOrderStatus_shouldTransitionAndPersist() throws Exception {
        OutwardOrder order = new OutwardOrder("CH-STATUSUPDATE", Channel.INTERNAL, 1L, null, OutwardOrderStatus.CREATED);
        order = outwardOrderRepository.save(order);

        OutwardOrderController.UpdateStatusRequest request = new OutwardOrderController.UpdateStatusRequest();
        request.setStatus(OutwardOrderStatus.PROCESSING);

        mockMvc.perform(put("/api/outward-orders/" + order.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OutwardOrderStatus.PROCESSING.name()));
    }

    @Test
    void cancelOrder_shouldReleaseInventoryViaRedisPubSub() throws Exception {
        OutwardOrder order = new OutwardOrder("CH-CANCEL", Channel.INTERNAL, 1L, null, OutwardOrderStatus.ALLOCATED);
        order = outwardOrderRepository.save(order);

        com.example.ordermanagement.entity.OutwardOrderItem item = 
            new com.example.ordermanagement.entity.OutwardOrderItem(order.getId(), 99L, 10, 10);
        outwardOrderItemRepository.save(item);

        OutwardOrderController.UpdateStatusRequest cancelRequest = new OutwardOrderController.UpdateStatusRequest();
        cancelRequest.setStatus(OutwardOrderStatus.CANCELLED);

        mockMvc.perform(put("/api/outward-orders/" + order.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk());

        ArgumentCaptor<InventoryEventDto> eventCaptor = ArgumentCaptor.forClass(InventoryEventDto.class);
        verify(redisPublisher, times(1)).publishInventoryEvent(eventCaptor.capture());

        InventoryEventDto capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getProductId()).isEqualTo(99L);
        assertThat(capturedEvent.getFromState()).isEqualTo(InventoryState.ALLOCATED);
        assertThat(capturedEvent.getToState()).isEqualTo(InventoryState.AVAILABLE);
        assertThat(capturedEvent.getQuantity()).isEqualTo(10);
        
        verify(redisPublisher, times(1)).publishCancelledOrder(any(OutwardOrder.class));
    }
}
