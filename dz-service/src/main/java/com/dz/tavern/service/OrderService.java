package com.dz.tavern.service;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.OrderEntity;
import com.dz.tavern.service.dto.OrderCreateCommand;
import com.dz.tavern.service.dto.OrderCreateResult;
import com.dz.tavern.service.dto.OrderDetail;

public interface OrderService {
    OrderCreateResult create(Long userId, OrderCreateCommand command);

    void cancel(Long userId, Long storeId, String orderNo);

    default void cancel(Long userId, String orderNo) {
        cancel(userId, null, orderNo);
    }

    PageResult<OrderEntity> page(Long userId, Long storeId, String status,
                                 long current, long size);

    default PageResult<OrderEntity> page(Long userId, String status, long current, long size) {
        return page(userId, null, status, current, size);
    }

    OrderDetail detail(Long userId, Long storeId, String orderNo);

    default OrderDetail detail(Long userId, String orderNo) {
        return detail(userId, null, orderNo);
    }

    void cancelTimeoutOrders();

    PageResult<OrderEntity> adminPage(Long storeId, String orderNo, String status,
                                      long current, long size);

    void complete(String orderNo);

    void markPaid(String orderNo);

    void refundBalanceOrder(String orderNo, String operator);

    void startRefund(String orderNo);

    void completeRefund(String orderNo);
}
