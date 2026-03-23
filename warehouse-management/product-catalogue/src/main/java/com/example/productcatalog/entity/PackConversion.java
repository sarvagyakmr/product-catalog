package com.example.productcatalog.entity;

import com.example.commons.enums.PackType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("PACK_CONVERSIONS")
public class PackConversion {

    @Id
    @Column("ID")
    private Long id;

    @Column("SKU_ID")
    private String skuId;

    @Column("FROM_PACK_TYPE")
    private PackType fromPackType;

    @Column("TO_PACK_TYPE")
    private PackType toPackType;

    @Column("CONVERSION_FACTOR")
    private Integer conversionFactor;

    public PackConversion() {
    }

    public PackConversion(String skuId, PackType fromPackType, PackType toPackType, Integer conversionFactor) {
        this.skuId = skuId;
        this.fromPackType = fromPackType;
        this.toPackType = toPackType;
        this.conversionFactor = conversionFactor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public PackType getFromPackType() {
        return fromPackType;
    }

    public void setFromPackType(PackType fromPackType) {
        this.fromPackType = fromPackType;
    }

    public PackType getToPackType() {
        return toPackType;
    }

    public void setToPackType(PackType toPackType) {
        this.toPackType = toPackType;
    }

    public Integer getConversionFactor() {
        return conversionFactor;
    }

    public void setConversionFactor(Integer conversionFactor) {
        this.conversionFactor = conversionFactor;
    }
}
