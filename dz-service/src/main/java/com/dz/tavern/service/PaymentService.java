package com.dz.tavern.service;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.PaymentRecordEntity;
import com.dz.tavern.service.dto.PaymentNotifyCommand;
import com.dz.tavern.service.dto.PrepayResult;

public interface PaymentService {
    PrepayResult prepay(Long userId, Long storeId, String orderNo);

    default PrepayResult prepay(Long userId, String orderNo) {
        return prepay(userId, null, orderNo);
    }

    PrepayResult prepayRecharge(Long userId, String rechargeNo);

    void handleNotify(PaymentNotifyCommand command);

    void refund(String orderNo, Long adminId);

    PageResult<PaymentRecordEntity> pageRecords(Long storeId, String orderNo,
                                                String tradeState, long current, long size);

    PaymentRecordEntity getRecord(String orderNo);
}
