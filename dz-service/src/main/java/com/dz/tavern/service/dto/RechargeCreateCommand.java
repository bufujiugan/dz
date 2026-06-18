package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotNull;

public record RechargeCreateCommand(@NotNull Long tierId) {
}
