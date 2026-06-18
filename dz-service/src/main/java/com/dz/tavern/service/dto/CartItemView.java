package com.dz.tavern.service.dto;

public record CartItemView(
        Long cartId,
        Long storeId,
        Long productId,
        Long skuId,
        String productName,
        String mainImage,
        String specName,
        Long priceFen,
        Integer stock,
        Integer quantity,
        Long subtotalFen) {
}
