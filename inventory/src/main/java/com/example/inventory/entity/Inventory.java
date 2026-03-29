package com.example.inventory.entity;

import com.example.commons.entity.BaseEntity;
import com.example.commons.enums.InventoryState;
import com.example.commons.enums.PackType;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("INVENTORY")
public class Inventory extends BaseEntity {

    @Column("PRODUCT_ID")
    private Long productId;

    @Column("PACK_TYPE")
    private PackType packType;

    @Column("QUANTITY")
    private Integer quantity;

    @Column("STATE")
    private InventoryState state;

    @Column("WAREHOUSE_ID")
    private Long warehouseId;

    public Inventory() {
    }

    public Inventory(Long productId, PackType packType, Integer quantity, InventoryState state, Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        this.productId = productId;
        this.packType = packType;
        this.quantity = quantity;
        this.state = state;
        this.warehouseId = warehouseId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public PackType getPackType() {
        return packType;
    }

    public void setPackType(PackType packType) {
        this.packType = packType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public InventoryState getState() {
        return state;
    }

    public void setState(InventoryState state) {
        this.state = state;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
