package com.example.productcatalog.entity;

import com.example.commons.enums.PackType;
import com.example.commons.enums.ProductType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("PRODUCTS")
public class Product {

    @Id
    @Column("ID")
    private Long id;
    @Column("CLIENT_ID")
    private Long clientId;
    @Column("SKU_ID")
    private String skuId;
    @Column("TYPE")
    private ProductType type;
    @Column("PACK_TYPE")
    private PackType packType;


    public Product() {
    }

    public Product(Long clientId, String skuId, ProductType type, PackType packType) {
        this.clientId = clientId;
        this.skuId = skuId;
        this.type = type;
        this.packType = packType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public ProductType getType() {
        return type;
    }

    public void setType(ProductType type) {
        this.type = type;
    }

    public PackType getPackType() {
        return packType;
    }

    public void setPackType(PackType packType) {
        this.packType = packType;
    }
}
