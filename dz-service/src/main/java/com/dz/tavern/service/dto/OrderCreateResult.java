package com.dz.tavern.service.dto;

import com.dz.tavern.common.enums.OrderStatus;

public record OrderCreateResult(String orderNo, Long totalFen, OrderStatus status) {
}
