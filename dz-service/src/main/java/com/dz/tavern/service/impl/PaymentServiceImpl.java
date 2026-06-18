package com.dz.tavern.service.impl;

import com.dz.tavern.common.annotation.Idempotent;
import com.dz.tavern.common.enums.OrderStatus;
import com.dz.tavern.common.enums.PayType;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.OrderEntity;
import com.dz.tavern.dao.entity.PaymentRecordEntity;
import com.dz.tavern.dao.entity.RechargeOrderEntity;
import com.dz.tavern.dao.entity.UserEntity;
import com.dz.tavern.dao.mapper.OrderMapper;
import com.dz.tavern.dao.mapper.PaymentRecordMapper;
import com.dz.tavern.dao.mapper.RechargeOrderMapper;
import com.dz.tavern.dao.mapper.UserMapper;
import com.dz.tavern.service.NotificationService;
import com.dz.tavern.service.OrderService;
import com.dz.tavern.service.PaymentService;
import com.dz.tavern.service.RechargeService;
import com.dz.tavern.service.dto.PaymentNotifyCommand;
import com.dz.tavern.service.dto.PrepayResult;
import com.dz.tavern.service.payment.WechatPayGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final OrderMapper orderMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final UserMapper userMapper;
    private final WechatPayGateway wechatPayGateway;
    private final OrderService orderService;
    private final RechargeService rechargeService;
    private final NotificationService notificationService;
    private final PaymentFailureRecorder paymentFailureRecorder;

    @Override
    public PrepayResult prepay(Long userId, Long storeId, String orderNo) {
        log.info("开始创建订单预支付 userId={} storeId={} orderNo={}", userId, storeId, orderNo);
        OrderEntity order = requireOrder(orderNo);
        if (!order.getUserId().equals(userId)
                || (storeId != null && !order.getStoreId().equals(storeId))
                || !OrderStatus.CREATED.name().equals(order.getStatus())
                || !PayType.WECHAT.name().equals(order.getPayType())) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        UserEntity user = requireUser(userId);
        log.info("调用微信预支付 userId={} orderNo={} amountFen={}",
                userId, orderNo, order.getTotalFen());
        PrepayResult result = wechatPayGateway.prepay(orderNo, order.getTotalFen(), user.getOpenid());
        orderMapper.markPrepayRequested(orderNo);
        log.info("订单预支付创建完成 userId={} orderNo={}", userId, orderNo);
        return result;
    }

    @Override
    public PrepayResult prepayRecharge(Long userId, String rechargeNo) {
        log.info("开始创建充值预支付 userId={} rechargeNo={}", userId, rechargeNo);
        RechargeOrderEntity order = rechargeOrderMapper.selectByRechargeNo(rechargeNo);
        if (order == null || !order.getUserId().equals(userId) || !"CREATED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        UserEntity user = requireUser(userId);
        log.info("调用微信充值预支付 userId={} rechargeNo={} amountFen={}",
                userId, rechargeNo, order.getPayFen());
        PrepayResult result = wechatPayGateway.prepay(
                rechargeNo, order.getPayFen(), user.getOpenid());
        rechargeOrderMapper.markPrepayRequested(rechargeNo);
        log.info("充值预支付创建完成 userId={} rechargeNo={}", userId, rechargeNo);
        return result;
    }

    @Override
    @Transactional
    @Idempotent(key = "#command.orderNo")
    public void handleNotify(PaymentNotifyCommand command) {
        log.info("收到支付回调 orderNo={} transactionId={}",
                command.orderNo(), command.transactionId());
        if (!command.signatureValid()) {
            paymentFailureRecorder.record(command, "VERIFY_FAILED");
            log.warn("支付回调验签失败 orderNo={} transactionId={}",
                    command.orderNo(), command.transactionId());
            throw new BizException(ErrorCode.PAYMENT_VERIFY_FAILED);
        }
        if (command.orderNo().startsWith("R")) {
            handleRechargeNotify(command);
        } else {
            handleOrderNotify(command);
        }
    }

    @Override
    @Transactional
    public void refund(String orderNo, Long adminId) {
        log.warn("管理员尝试线上退款 adminId={} orderNo={}", adminId, orderNo);
        throw new BizException(ErrorCode.ONLINE_REFUND_DISABLED);
    }

    @Override
    public PageResult<PaymentRecordEntity> pageRecords(Long storeId, String orderNo,
                                                       String tradeState, long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                paymentRecordMapper.countPage(storeId, orderNo, tradeState),
                paymentRecordMapper.selectPage(storeId, orderNo, tradeState, offset, normalizedSize));
    }

    @Override
    public PaymentRecordEntity getRecord(String orderNo) {
        PaymentRecordEntity record = paymentRecordMapper.selectByOrderNo(orderNo);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return record;
    }

    private void handleOrderNotify(PaymentNotifyCommand command) {
        OrderEntity order = requireOrder(command.orderNo());
        UserEntity user = requireUser(order.getUserId());
        validatePayment(command, order.getTotalFen(), user.getOpenid());
        if (!OrderStatus.PAID.name().equals(order.getStatus())) {
            orderService.markPaid(order.getOrderNo());
        }
        saveOrUpdateRecord(command, "SUCCESS", "SUCCESS");
        notificationService.createTask(order.getUserId(), "PAY_SUCCESS",
                "{\"orderNo\":\"" + order.getOrderNo() + "\"}");
        log.info("订单支付回调处理完成 orderNo={} userId={} transactionId={}",
                order.getOrderNo(), order.getUserId(), command.transactionId());
    }

    private void handleRechargeNotify(PaymentNotifyCommand command) {
        RechargeOrderEntity order = rechargeOrderMapper.selectByRechargeNo(command.orderNo());
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        UserEntity user = requireUser(order.getUserId());
        validatePayment(command, order.getPayFen(), user.getOpenid());
        rechargeService.credit(order.getRechargeNo(), "WECHAT");
        saveOrUpdateRecord(command, "SUCCESS", "SUCCESS");
        log.info("充值支付回调处理完成 rechargeNo={} userId={} transactionId={}",
                order.getRechargeNo(), order.getUserId(), command.transactionId());
    }

    private void validatePayment(PaymentNotifyCommand command, Long expectedAmount,
                                 String expectedOpenid) {
        if (!expectedAmount.equals(command.amountFen())) {
            paymentFailureRecorder.record(command, "AMOUNT_MISMATCH");
            log.error("支付金额校验失败 orderNo={} expectedFen={} actualFen={}",
                    command.orderNo(), expectedAmount, command.amountFen());
            throw new BizException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        if (!expectedOpenid.equals(command.openid())) {
            paymentFailureRecorder.record(command, "OWNER_MISMATCH");
            log.error("支付用户校验失败 orderNo={}", command.orderNo());
            throw new BizException(ErrorCode.PAYMENT_OWNER_MISMATCH);
        }
    }

    private void saveOrUpdateRecord(PaymentNotifyCommand command,
                                    String verifyResult, String tradeState) {
        PaymentRecordEntity existing = paymentRecordMapper.selectByOrderNo(command.orderNo());
        if (existing != null) {
            // 微信可能重复通知，同一业务单只累加通知次数，不重复推进业务状态。
            paymentRecordMapper.increaseNotifyCount(
                    command.orderNo(), command.rawBody(), verifyResult);
            return;
        }
        PaymentRecordEntity record = new PaymentRecordEntity();
        record.setOrderNo(command.orderNo());
        record.setTransactionId(command.transactionId());
        record.setAmountFen(command.amountFen() == null ? 0L : command.amountFen());
        record.setOpenid(command.openid() == null ? "" : command.openid());
        record.setTradeState(tradeState);
        record.setNotifyRaw(command.rawBody());
        record.setVerifyResult(verifyResult);
        record.setNotifyCount(1);
        paymentRecordMapper.insertRecord(record);
    }

    private OrderEntity requireOrder(String orderNo) {
        OrderEntity order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return order;
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return user;
    }
}
