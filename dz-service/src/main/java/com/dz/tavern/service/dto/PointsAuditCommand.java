package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PointsAuditCommand(
        @NotNull Long requestId,
        boolean approve,
        @Size(max = 255) String auditRemark) {
}
