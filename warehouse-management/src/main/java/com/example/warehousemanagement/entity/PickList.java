package com.example.warehousemanagement.entity;

import com.example.warehousemanagement.enums.PickListStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("PICK_LISTS")
public class PickList {

    @Id
    @Column("ID")
    private Long id;

    @Column("ORDER_ID")
    private Long orderId;

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("STORAGE_BOX_ID")
    private Long storageBoxId;

    @Column("STATUS")
    private PickListStatus status;

    public PickList() {
    }

    public PickList(Long orderId, Long productId, Long storageBoxId, PickListStatus status) {
        this.orderId = orderId;
        this.productId = productId;
        this.storageBoxId = storageBoxId;
        this.status = status;
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

    public Long getStorageBoxId() {
        return storageBoxId;
    }

    public void setStorageBoxId(Long storageBoxId) {
        this.storageBoxId = storageBoxId;
    }

    public PickListStatus getStatus() {
        return status;
    }

    public void setStatus(PickListStatus status) {
        this.status = status;
    }
}
