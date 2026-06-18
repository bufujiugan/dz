package com.dz.tavern.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartCommand(
        Long id,
        Long storeId,
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity) {
}
