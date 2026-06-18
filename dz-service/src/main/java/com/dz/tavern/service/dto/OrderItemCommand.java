package com.dz.tavern.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemCommand(
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity,
        Long expectedPriceFen) {
}
