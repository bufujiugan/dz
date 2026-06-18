package com.dz.tavern.service.impl;

import com.dz.tavern.dao.entity.PaymentRecordEntity;
import com.dz.tavern.dao.mapper.PaymentRecordMapper;
import com.dz.tavern.service.dto.PaymentNotifyCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailureRecorder {
    private final PaymentRecordMapper paymentRecordMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(PaymentNotifyCommand command, String verifyResult) {
        // 使用独立事务保留失败回调证据，外层支付事务回滚时该记录仍然提交。
        PaymentRecordEntity existing = paymentRecordMapper.selectByOrderNo(command.orderNo());
        if (existing != null) {
            paymentRecordMapper.increaseNotifyCount(
                    command.orderNo(), command.rawBody(), verifyResult);
            log.warn("支付失败回调已更新 orderNo={} transactionId={} verifyResult={}",
                    command.orderNo(), command.transactionId(), verifyResult);
            return;
        }
        PaymentRecordEntity record = new PaymentRecordEntity();
        record.setOrderNo(command.orderNo());
        record.setTransactionId(command.transactionId());
        record.setAmountFen(command.amountFen() == null ? 0L : command.amountFen());
        record.setOpenid(command.openid() == null ? "" : command.openid());
        record.setTradeState("FAIL");
        record.setNotifyRaw(command.rawBody());
        record.setVerifyResult(verifyResult);
        record.setNotifyCount(1);
        paymentRecordMapper.insertRecord(record);
        log.warn("支付失败回调已记录 orderNo={} transactionId={} verifyResult={}",
                command.orderNo(), command.transactionId(), verifyResult);
    }
}
