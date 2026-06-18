package com.dz.tavern.service;

import com.dz.tavern.common.enums.OrderStatus;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.COMPLETED, OrderStatus.REFUNDING),
            OrderStatus.COMPLETED, Set.of(OrderStatus.REFUNDING),
            OrderStatus.REFUNDING, Set.of(OrderStatus.REFUNDED)
    );

    public void validate(OrderStatus current, OrderStatus target) {
        // 状态只能沿预定义方向推进，退款完成后等终态不允许再次流转。
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(target)) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
    }
}
