package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StorePointsApplyCommand(
        @NotNull Long storeId,
        @NotNull @Positive Long points,
        @NotBlank @Size(max = 255) String remark) {
}
