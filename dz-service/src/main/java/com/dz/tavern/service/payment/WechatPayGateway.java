package com.dz.tavern.service.payment;

import com.dz.tavern.service.dto.PrepayResult;

public interface WechatPayGateway {
    PrepayResult prepay(String bizNo, long amountFen, String openid);

    void close(String bizNo);

    void refund(String orderNo, long amountFen);
}
