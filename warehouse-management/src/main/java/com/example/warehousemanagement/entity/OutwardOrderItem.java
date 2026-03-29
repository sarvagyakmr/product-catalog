package com.example.warehousemanagement.entity;

import com.example.commons.entity.BaseEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("WM_OUTWARD_ORDER_ITEMS")
public class OutwardOrderItem extends BaseEntity {

    @Column("ORDER_ID")
    private Long orderId;

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("QUANTITY")
    private Integer quantity;

    @Column("PICK_LIST_COUNT")
    private Integer pickListCount;

    @Column("PICKED_ITEM_ID")
    private Long pickedItemId;

    public OutwardOrderItem() {
    }

    public OutwardOrderItem(Long orderId, Long productId, Integer quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.pickListCount = 0;
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

    public Long getPickedItemId() {
        return pickedItemId;
    }

    public void setPickedItemId(Long pickedItemId) {
        this.pickedItemId = pickedItemId;
    }
}
