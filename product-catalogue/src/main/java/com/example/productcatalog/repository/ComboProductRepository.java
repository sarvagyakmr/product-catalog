package com.example.productcatalog.repository;

import com.example.productcatalog.entity.ComboProduct;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboProductRepository extends CrudRepository<ComboProduct, Long> {
    List<ComboProduct> findAll();
    List<ComboProduct> findByComboProductId(Long comboProductId);
}

