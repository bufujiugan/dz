package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountAdjustCommand(
        @NotNull Long userId,
        @NotNull AssetType type,
        @NotNull Long value,
        @NotBlank String remark,
        boolean confirm) {

    public enum AssetType {
        BALANCE,
        POINTS
    }
}
