package com.example.productcatalog.entity;

import com.example.commons.entity.BaseEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("COMBO_PRODUCTS")
public class ComboProduct extends BaseEntity {
    
    @Column("COMBO_PRODUCT_ID")
    private Long comboProductId;
    
    @Column("PRODUCT_ID")
    private Long productId;
    
    @Column("QUANTITY")
    private Integer quantity;

    public ComboProduct() {
    }

    public ComboProduct(Long comboProductId, Long productId, Integer quantity) {
        this.comboProductId = comboProductId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getComboProductId() {
        return comboProductId;
    }

    public void setComboProductId(Long comboProductId) {
        this.comboProductId = comboProductId;
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
}

