package com.example.productcatalog.repository;

import com.example.commons.enums.PackType;
import com.example.productcatalog.entity.PackConversion;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PackConversionRepository extends CrudRepository<PackConversion, Long> {
    Optional<PackConversion> findBySkuIdAndFromPackTypeAndToPackType(String skuId, PackType fromPackType, PackType toPackType);
}
