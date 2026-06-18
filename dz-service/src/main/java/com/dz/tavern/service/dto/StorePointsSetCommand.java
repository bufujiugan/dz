package com.dz.tavern.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StorePointsSetCommand(
        @NotNull Long storeId,
        @NotNull Long userId,
        @NotNull @Min(0) Long points,
        @Size(max = 255) String remark) {
}
