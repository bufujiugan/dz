package com.dz.tavern.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CouponGiftCommand(
        @NotNull Long templateId,
        @NotEmpty List<Long> userIds,
        @Min(1) @Max(20) Integer quantity) {
}
