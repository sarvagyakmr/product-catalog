package com.example.commons.dto;

import com.example.commons.enums.PackType;

public class PackConversionDto {
    private Long id;
    private String skuId;
    private PackType fromPackType;
    private PackType toPackType;
    private Integer conversionFactor;

    public static class PackConversionQuery {
        private PackType fromPackType;
        private PackType toPackType;

        public PackConversionQuery() {
        }

        public PackConversionQuery(PackType fromPackType, PackType toPackType) {
            this.fromPackType = fromPackType;
            this.toPackType = toPackType;
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
