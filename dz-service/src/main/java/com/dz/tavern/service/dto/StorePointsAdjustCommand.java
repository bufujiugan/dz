package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StorePointsAdjustCommand(
        @NotNull Long storeId,
        @NotNull Long userId,
        @NotNull Long value,
        @Size(max = 255) String remark) {
}
