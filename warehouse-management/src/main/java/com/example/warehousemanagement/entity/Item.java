package com.example.warehousemanagement.entity;

import com.example.warehousemanagement.enums.ItemStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ITEMS")
public class Item {

    @Id
    @Column("ID")
    private Long id;

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("STATUS")
    private ItemStatus status;

    @Column("WAREHOUSE_ID")
    private Long warehouseId;

    public Item() {
    }

    public Item(Long productId, ItemStatus status) {
        this.productId = productId;
        this.status = status;
    }

    public Item(Long productId, ItemStatus status, Long warehouseId) {
        this.productId = productId;
        this.status = status;
        this.warehouseId = warehouseId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
