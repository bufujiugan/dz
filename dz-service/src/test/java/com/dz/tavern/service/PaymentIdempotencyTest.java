package com.dz.tavern.service;

import com.dz.tavern.common.enums.OrderStatus;
import com.dz.tavern.common.enums.PayType;
import com.dz.tavern.dao.entity.OrderEntity;
import com.dz.tavern.dao.entity.UserEntity;
import com.dz.tavern.dao.mapper.IdempotentRecordMapper;
import com.dz.tavern.dao.mapper.OrderMapper;
import com.dz.tavern.dao.mapper.PaymentRecordMapper;
import com.dz.tavern.dao.mapper.RechargeOrderMapper;
import com.dz.tavern.dao.mapper.UserMapper;
import com.dz.tavern.service.aspect.IdempotentAspect;
import com.dz.tavern.service.dto.PaymentNotifyCommand;
import com.dz.tavern.service.impl.PaymentFailureRecorder;
import com.dz.tavern.service.impl.PaymentServiceImpl;
import com.dz.tavern.service.payment.WechatPayGateway;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentIdempotencyTest {

    @Test
    void duplicateNotifyShouldOnlyCreditOnce() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        RechargeOrderMapper rechargeOrderMapper = mock(RechargeOrderMapper.class);
        PaymentRecordMapper paymentRecordMapper = mock(PaymentRecordMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        WechatPayGateway gateway = mock(WechatPayGateway.class);
        OrderService orderService = mock(OrderService.class);
        RechargeService rechargeService = mock(RechargeService.class);
        NotificationService notificationService = mock(NotificationService.class);
        PaymentFailureRecorder failureRecorder = mock(PaymentFailureRecorder.class);

        OrderEntity order = new OrderEntity();
        order.setOrderNo("O20260610000000123456");
        order.setUserId(1L);
        order.setTotalFen(100L);
        order.setPayType(PayType.WECHAT.name());
        order.setStatus(OrderStatus.CREATED.name());
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setOpenid("mock-openid-demo");
        when(orderMapper.selectByOrderNo(order.getOrderNo())).thenReturn(order);
        when(userMapper.selectById(1L)).thenReturn(user);
        doAnswer(invocation -> {
            order.setStatus(OrderStatus.PAID.name());
            return null;
        }).when(orderService).markPaid(order.getOrderNo());
        when(paymentRecordMapper.selectByOrderNo(order.getOrderNo())).thenReturn(null);
        when(paymentRecordMapper.insertRecord(any())).thenReturn(1);

        PaymentServiceImpl target = new PaymentServiceImpl(
                orderMapper, rechargeOrderMapper, paymentRecordMapper, userMapper, gateway,
                orderService, rechargeService, notificationService, failureRecorder);
        IdempotentRecordMapper idempotentMapper = idempotentMapper();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new IdempotentAspect(idempotentMapper));
        PaymentService proxy = factory.getProxy();

        PaymentNotifyCommand command = new PaymentNotifyCommand(
                order.getOrderNo(), "TX-1", 100L, user.getOpenid(), "{}", true);
        proxy.handleNotify(command);
        proxy.handleNotify(command);

        verify(orderService, times(1)).markPaid(order.getOrderNo());
        verify(paymentRecordMapper, times(1)).insertRecord(any());
        verify(notificationService, times(1))
                .createTask(1L, "PAY_SUCCESS", "{\"orderNo\":\"" + order.getOrderNo() + "\"}");
    }

    private IdempotentRecordMapper idempotentMapper() {
        IdempotentRecordMapper mapper = mock(IdempotentRecordMapper.class);
        AtomicBoolean acquired = new AtomicBoolean();
        AtomicBoolean success = new AtomicBoolean();
        when(mapper.tryAcquire(anyString(), any(LocalDateTime.class)))
                .thenAnswer(invocation -> acquired.compareAndSet(false, true) ? 1 : 0);
        when(mapper.markSuccess(anyString())).thenAnswer(invocation -> {
            success.set(true);
            return 1;
        });
        when(mapper.selectStatus(anyString()))
                .thenAnswer(invocation -> success.get() ? "SUCCESS" : "PROCESSING");
        return mapper;
    }
}
