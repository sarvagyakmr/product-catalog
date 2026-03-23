package com.example.ordermanagement.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("INWARD_ORDER_ITEMS")
public class InwardOrderItem {

    @Id
    @Column("ID")
    private Long id;

    @Column("ORDER_ID")
    private Long orderId;

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("ORDERED_QUANTITY")
    private Integer orderedQuantity;

    @Column("RECEIVED_QUANTITY")
    private Integer receivedQuantity;

    public InwardOrderItem() {
    }

    public InwardOrderItem(Long orderId, Long productId, Integer orderedQuantity, Integer receivedQuantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.orderedQuantity = orderedQuantity;
        this.receivedQuantity = receivedQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(Integer orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }
}
