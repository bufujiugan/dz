package com.dz.tavern.service.dto;

import com.dz.tavern.common.enums.PayType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderCreateCommand(
        @NotNull Long storeId,
        @NotEmpty List<@Valid OrderItemCommand> items,
        @NotNull PayType payType,
        @Size(max = 255) String remark) {
}
