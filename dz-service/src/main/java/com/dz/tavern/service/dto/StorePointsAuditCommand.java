package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StorePointsAuditCommand(
        @NotNull Long requestId,
        @NotNull Boolean approve,
        @Size(max = 255) String auditRemark) {
}
