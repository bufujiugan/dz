package com.dz.tavern.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CouponRedeemCommand(
        @NotNull Long userCouponId,
        @Size(max = 255) String remark) {
}
