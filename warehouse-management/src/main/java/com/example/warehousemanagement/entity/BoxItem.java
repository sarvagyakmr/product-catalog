package com.example.warehousemanagement.entity;

import com.example.commons.entity.BaseEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("BOX_ITEMS")
public class BoxItem extends BaseEntity {

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
