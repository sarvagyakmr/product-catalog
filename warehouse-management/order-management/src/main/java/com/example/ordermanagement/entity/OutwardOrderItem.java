package com.example.ordermanagement.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("OUTWARD_ORDER_ITEMS")
public class OutwardOrderItem {

    @Id
    @Column("ID")
    private Long id;

    @Column("ORDER_ID")
    private Long orderId;

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("ORDERED_QUANTITY")
    private Integer orderedQuantity;

    @Column("ALLOCATED_QUANTITY")
    private Integer allocatedQuantity;

    public OutwardOrderItem() {
    }

    public OutwardOrderItem(Long orderId, Long productId, Integer orderedQuantity, Integer allocatedQuantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.orderedQuantity = orderedQuantity;
        this.allocatedQuantity = allocatedQuantity;
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

    public Integer getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(Integer allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }
}
