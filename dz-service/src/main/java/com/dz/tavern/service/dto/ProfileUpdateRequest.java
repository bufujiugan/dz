package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 64) String nickname,
        @Size(max = 512) String avatar) {
}
