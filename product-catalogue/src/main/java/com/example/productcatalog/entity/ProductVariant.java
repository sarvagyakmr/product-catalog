package com.example.productcatalog.entity;

import com.example.commons.entity.BaseEntity;
import com.example.commons.enums.VariantType;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("PRODUCT_VARIANTS")
public class ProductVariant extends BaseEntity {

    @Column("PRODUCT_ID")
    private Long productId;
    @Column("VARIANT_PRODUCT_ID")
    private Long variantProductId;
    @Column("VARIANT_TYPE")
    private VariantType variantType;


    public ProductVariant() {
    }

    public ProductVariant(Long productId, Long variantProductId, VariantType variantType) {
        this.productId = productId;
        this.variantProductId = variantProductId;
        this.variantType = variantType;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getVariantProductId() {
        return variantProductId;
    }

    public void setVariantProductId(Long variantProductId) {
        this.variantProductId = variantProductId;
    }

    public VariantType getVariantType() {
        return variantType;
    }

    public void setVariantType(VariantType variantType) {
        this.variantType = variantType;
    }
}
