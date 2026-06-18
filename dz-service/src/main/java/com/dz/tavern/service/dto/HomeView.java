package com.dz.tavern.service.dto;

import com.dz.tavern.dao.entity.ProductEntity;

import java.util.List;

public record HomeView(
        String announcement,
        String businessEndTime,
        String homeSlogan,
        String heroImage,
        String gameplayDescription,
        String menuTitle,
        List<ProductEntity> recommendedProducts) {
}
