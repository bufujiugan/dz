package com.dz.tavern.service.payment;

import com.dz.tavern.service.dto.PrepayResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Component
@ConditionalOnProperty(name = "wechat.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockWechatPayGateway implements WechatPayGateway {
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public PrepayResult prepay(String bizNo, long amountFen, String openid) {
        byte[] nonce = new byte[16];
        secureRandom.nextBytes(nonce);
        String nonceStr = HexFormat.of().formatHex(nonce);
        log.info("mock微信预支付已创建 bizNo={} amountFen={}", bizNo, amountFen);
        return new PrepayResult(
                String.valueOf(Instant.now().getEpochSecond()),
                nonceStr,
                "prepay_id=mock_" + bizNo,
                "RSA",
                "MOCK_PAY_SIGN_" + nonceStr);
    }

    @Override
    public void close(String bizNo) {
        log.info("mock微信订单已关闭 bizNo={}", bizNo);
    }

    @Override
    public void refund(String orderNo, long amountFen) {
        log.info("mock微信退款已受理 orderNo={} amountFen={}", orderNo, amountFen);
    }
}
