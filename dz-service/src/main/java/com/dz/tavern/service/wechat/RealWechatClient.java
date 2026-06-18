package com.dz.tavern.service.wechat;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.dao.entity.WechatAccessTokenEntity;
import com.dz.tavern.dao.mapper.WechatAccessTokenMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wechat.auth-mock-enabled", havingValue = "false")
public class RealWechatClient implements WechatClient {
    private static final String TOKEN_TYPE = "MINI_PROGRAM";
    private static final String CODE_TO_SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";
    private static final String ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_NUMBER_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final long TOKEN_REFRESH_ADVANCE_SECONDS = 120L;

    private final WechatAccessTokenMapper accessTokenMapper;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private final Object tokenRefreshLock = new Object();

    @Value("${wechat.app-id:}")
    private String appId;

    @Value("${wechat.app-secret:}")
    private String appSecret;

    @Override
    public SessionResult code2Session(String code) {
        validateWechatConfig();
        log.info("开始调用微信 code2Session");
        String url = CODE_TO_SESSION_URL
                + "?appid=" + encode(appId)
                + "&secret=" + encode(appSecret)
                + "&js_code=" + encode(code)
                + "&grant_type=authorization_code";
        JsonNode response = sendGet(url, "code2Session");
        checkWechatError(response, "code2Session");
        String openid = response.path("openid").asText("");
        if (openid.isBlank()) {
            log.warn("微信 code2Session 未返回 openid");
            throw new BizException(ErrorCode.WECHAT_API_ERROR);
        }
        log.info("微信 code2Session 调用成功");
        return new SessionResult(openid, nullableText(response.path("unionid")));
    }

    @Override
    public String getPhoneNumber(String code) {
        validateWechatConfig();
        log.info("开始调用微信手机号授权接口");
        String accessToken = getAccessToken();
        String url = PHONE_NUMBER_URL + "?access_token=" + encode(accessToken);
        JsonNode response = sendPost(url, Map.of("code", code), "getPhoneNumber");
        checkWechatError(response, "getPhoneNumber");
        String phoneNumber = response.path("phone_info").path("phoneNumber").asText("");
        if (phoneNumber.isBlank()) {
            log.warn("微信手机号授权接口未返回手机号");
            throw new BizException(ErrorCode.WECHAT_API_ERROR);
        }
        log.info("微信手机号授权接口调用成功");
        return phoneNumber;
    }

    @Override
    public void sendSubscribeMessage(String openid, String templateType, String jsonData) {
        // 订阅消息需要按模板类型配置 templateId；未配置前仅保留任务，不发送不完整请求。
        log.warn("微信订阅消息模板尚未配置 templateType={}", templateType);
    }

    private String getAccessToken() {
        WechatAccessTokenEntity cached = accessTokenMapper.selectByTokenType(TOKEN_TYPE);
        if (isTokenAvailable(cached)) {
            return cached.getAccessToken();
        }
        synchronized (tokenRefreshLock) {
            cached = accessTokenMapper.selectByTokenType(TOKEN_TYPE);
            if (isTokenAvailable(cached)) {
                return cached.getAccessToken();
            }
            log.info("开始刷新微信 access token tokenType={}", TOKEN_TYPE);
            String url = ACCESS_TOKEN_URL
                    + "?grant_type=client_credential"
                    + "&appid=" + encode(appId)
                    + "&secret=" + encode(appSecret);
            JsonNode response = sendGet(url, "accessToken");
            checkWechatError(response, "accessToken");
            String accessToken = response.path("access_token").asText("");
            long expiresIn = response.path("expires_in").asLong(0);
            if (accessToken.isBlank() || expiresIn <= TOKEN_REFRESH_ADVANCE_SECONDS) {
                log.warn("微信 access token 响应字段不完整");
                throw new BizException(ErrorCode.WECHAT_API_ERROR);
            }
            WechatAccessTokenEntity refreshed = new WechatAccessTokenEntity();
            refreshed.setTokenType(TOKEN_TYPE);
            refreshed.setAccessToken(accessToken);
            refreshed.setExpireTime(LocalDateTime.now().plusSeconds(
                    expiresIn - TOKEN_REFRESH_ADVANCE_SECONDS));
            accessTokenMapper.upsert(refreshed);
            log.info("微信 access token 刷新完成 tokenType={}", TOKEN_TYPE);
            return accessToken;
        }
    }

    private boolean isTokenAvailable(WechatAccessTokenEntity accessToken) {
        return accessToken != null
                && accessToken.getAccessToken() != null
                && !accessToken.getAccessToken().isBlank()
                && accessToken.getExpireTime() != null
                && accessToken.getExpireTime().isAfter(LocalDateTime.now());
    }

    private JsonNode sendGet(String url, String operation) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        return send(request, operation);
    }

    private JsonNode sendPost(String url, Object body, String operation) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            return send(request, operation);
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("构造微信接口请求失败 operation={}", operation, exception);
            throw new BizException(ErrorCode.WECHAT_API_ERROR);
        }
    }

    private JsonNode send(HttpRequest request, String operation) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("微信接口 HTTP 状态异常 operation={} statusCode={}",
                        operation, response.statusCode());
                throw new BizException(ErrorCode.WECHAT_API_ERROR);
            }
            return objectMapper.readTree(response.body());
        } catch (BizException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("微信接口调用被中断 operation={}", operation, exception);
            throw new BizException(ErrorCode.WECHAT_API_ERROR);
        } catch (Exception exception) {
            log.error("微信接口调用异常 operation={}", operation, exception);
            throw new BizException(ErrorCode.WECHAT_API_ERROR);
        }
    }

    private void checkWechatError(JsonNode response, String operation) {
        int errorCode = response.path("errcode").asInt(0);
        if (errorCode != 0) {
            log.warn("微信接口返回业务错误 operation={} errcode={}", operation, errorCode);
            throw new BizException(ErrorCode.WECHAT_API_ERROR);
        }
    }

    private void validateWechatConfig() {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            log.error("微信小程序配置缺失 appIdConfigured={} appSecretConfigured={}",
                    appId != null && !appId.isBlank(),
                    appSecret != null && !appSecret.isBlank());
            throw new BizException(ErrorCode.WECHAT_CONFIG_MISSING);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String nullableText(JsonNode node) {
        String value = node.asText("");
        return value.isBlank() ? null : value;
    }
}
