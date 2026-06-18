package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneBindRequest(@NotBlank String code) {
}
