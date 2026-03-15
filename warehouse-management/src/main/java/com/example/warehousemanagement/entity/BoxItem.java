package com.example.warehousemanagement.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("BOX_ITEMS")
public class BoxItem {

    @Id
    @Column("ID")
    private Long id;

    @Column("ITEM_ID")
    private Long itemId;

    @Column("BOX_ID")
    private Long boxId;

    public BoxItem() {
    }

    public BoxItem(Long itemId, Long boxId) {
        this.itemId = itemId;
        this.boxId = boxId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }
}
