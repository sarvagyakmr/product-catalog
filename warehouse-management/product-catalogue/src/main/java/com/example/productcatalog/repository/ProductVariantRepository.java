package com.example.productcatalog.repository;

import com.example.productcatalog.entity.ProductVariant;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends CrudRepository<ProductVariant, Long> {
    List<ProductVariant> findAll();
    List<ProductVariant> findByProductId(Long productId);
}
