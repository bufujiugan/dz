package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(@NotBlank String code, @NotNull Long storeId) {
}
