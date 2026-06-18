package com.dz.tavern.service.impl;

import com.dz.tavern.common.enums.OrderStatus;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.dao.entity.OrderEntity;
import com.dz.tavern.dao.entity.RechargeOrderEntity;
import com.dz.tavern.dao.entity.UserEntity;
import com.dz.tavern.dao.mapper.OrderMapper;
import com.dz.tavern.dao.mapper.RechargeOrderMapper;
import com.dz.tavern.dao.mapper.UserMapper;
import com.dz.tavern.service.MockPaymentService;
import com.dz.tavern.service.PaymentService;
import com.dz.tavern.service.dto.PaymentNotifyCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockPaymentServiceImpl implements MockPaymentService {
    private final OrderMapper orderMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final UserMapper userMapper;
    private final PaymentService paymentService;

    @Override
    public void simulateSuccess(Long userId, String bizNo) {
        PaymentNotifyCommand command = bizNo.startsWith("R")
                ? buildRechargeNotify(userId, bizNo)
                : buildOrderNotify(userId, bizNo);
        log.info("开始模拟支付成功 userId={} bizNo={}", userId, bizNo);
        // 通过独立服务调用 PaymentService，确保支付回调的事务和幂等切面正常生效。
        paymentService.handleNotify(command);
        log.info("模拟支付成功处理完成 userId={} bizNo={}", userId, bizNo);
    }

    private PaymentNotifyCommand buildOrderNotify(Long userId, String orderNo) {
        OrderEntity order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!OrderStatus.CREATED.name().equals(order.getStatus())
                && !OrderStatus.PAID.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        return createNotify(orderNo, order.getTotalFen(), requireUser(userId).getOpenid());
    }

    private PaymentNotifyCommand buildRechargeNotify(Long userId, String rechargeNo) {
        RechargeOrderEntity order = rechargeOrderMapper.selectByRechargeNo(rechargeNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!"CREATED".equals(order.getStatus()) && !"SUCCESS".equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        return createNotify(rechargeNo, order.getPayFen(), requireUser(userId).getOpenid());
    }

    private PaymentNotifyCommand createNotify(String bizNo, Long amountFen, String openid) {
        return new PaymentNotifyCommand(
                bizNo,
                "MOCK_" + bizNo,
                amountFen,
                openid,
                "{\"source\":\"mini-program-mock\"}",
                true);
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return user;
    }
}
