package com.dz.tavern.service.wechat;

public interface WechatClient {
    SessionResult code2Session(String code);

    String getPhoneNumber(String code);

    void sendSubscribeMessage(String openid, String templateType, String jsonData);

    record SessionResult(String openid, String unionid) {
    }
}
