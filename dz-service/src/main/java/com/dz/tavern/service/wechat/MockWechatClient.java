package com.dz.tavern.service.wechat;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "wechat.auth-mock-enabled", havingValue = "true",
        matchIfMissing = true)
public class MockWechatClient implements WechatClient {

    @Override
    public SessionResult code2Session(String code) {
        String suffix = code.replaceAll("[^a-zA-Z0-9]", "");
        if (suffix.isBlank() || "demo".equalsIgnoreCase(suffix)) {
            return new SessionResult("mock-openid-demo", "mock-unionid-demo");
        }
        return new SessionResult("mock-openid-" + suffix, "mock-unionid-" + suffix);
    }

    @Override
    public String getPhoneNumber(String code) {
        String digits = code.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits;
        }
        log.warn("mock微信手机号授权无法返回真实手机号 codeLength={}", code.length());
        throw new BizException(ErrorCode.WECHAT_API_ERROR);
    }

    @Override
    public void sendSubscribeMessage(String openid, String templateType, String jsonData) {
        log.info("mock订阅消息已发送 openidMasked={} templateType={}",
                mask(openid), templateType);
    }

    private String mask(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
