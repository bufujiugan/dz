package com.dz.tavern.service.dto;

import com.dz.tavern.dao.entity.ProductEntity;
import com.dz.tavern.dao.entity.ProductSkuEntity;

import java.util.List;

public record ProductDetail(ProductEntity product, List<ProductSkuEntity> skus) {
}
