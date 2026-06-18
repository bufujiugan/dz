package com.dz.tavern.service.dto;

import com.dz.tavern.dao.entity.ProductSkuEntity;

import java.util.List;

public record MenuProductView(
        Long id,
        Long storeId,
        Long categoryId,
        String name,
        String mainImage,
        String description,
        Integer recommended,
        Long minPriceFen,
        Integer sales,
        List<ProductSkuEntity> skus) {
}
