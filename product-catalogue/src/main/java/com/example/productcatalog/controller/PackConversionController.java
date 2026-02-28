package com.example.productcatalog.controller;

import com.example.productcatalog.entity.PackConversion;
import com.example.productcatalog.enums.PackType;
import com.example.productcatalog.repository.PackConversionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pack-conversions")
public class PackConversionController {

    private static final Logger log = LoggerFactory.getLogger(PackConversionController.class);

    private final PackConversionRepository packConversionRepository;

    public PackConversionController(PackConversionRepository packConversionRepository) {
        this.packConversionRepository = packConversionRepository;
    }

    @PostMapping
    public ResponseEntity<PackConversion> createPackConversion(@RequestBody PackConversion packConversion) {
        log.info("Creating pack conversion for sku: {}, from: {}, to: {}, factor: {}",
                packConversion.getSkuId(), packConversion.getFromPackType(),
                packConversion.getToPackType(), packConversion.getConversionFactor());
        packConversion.setId(null);
        PackConversion savedPackConversion = packConversionRepository.save(packConversion);
        return new ResponseEntity<>(savedPackConversion, HttpStatus.CREATED);
    }

    @GetMapping("/{skuId}")
    public ResponseEntity<PackConversion> getPackConversion(
            @PathVariable("skuId") String skuId,
            @RequestParam("fromPackType") PackType fromPackType,
            @RequestParam("toPackType") PackType toPackType) {
        return packConversionRepository.findBySkuIdAndFromPackTypeAndToPackType(skuId, fromPackType, toPackType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
