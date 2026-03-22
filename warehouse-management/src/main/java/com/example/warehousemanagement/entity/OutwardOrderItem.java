package com.example.warehousemanagement.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("WM_OUTWARD_ORDER_ITEMS")
public class OutwardOrderItem {

    @Id
    @Column("ID")
    private Long id;

    @Column("ORDER_ID")
    private Long orderId;

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("QUANTITY")
    private Integer quantity;

    @Column("PICK_LIST_COUNT")
    private Integer pickListCount;

    public OutwardOrderItem() {
    }

    public OutwardOrderItem(Long orderId, Long productId, Integer quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.pickListCount = 0;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPickListCount() {
        return pickListCount;
    }

    public void setPickListCount(Integer pickListCount) {
        this.pickListCount = pickListCount;
    }
}
