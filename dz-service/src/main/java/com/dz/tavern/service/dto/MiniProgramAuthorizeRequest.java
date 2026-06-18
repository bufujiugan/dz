package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MiniProgramAuthorizeRequest(
        @NotBlank String loginCode,
        @NotNull Long storeId,
        String phoneCode,
        @Size(max = 64) String nickname,
        @Size(max = 512) String avatar) {
}
