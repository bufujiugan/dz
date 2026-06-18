package com.dz.tavern.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INVALID_PARAMETER(40001, "请求参数不合法"),
    UNAUTHORIZED(40100, "请先登录"),
    TOKEN_INVALID(40101, "登录状态已失效"),
    FORBIDDEN(40300, "无权执行该操作"),
    USER_BANNED(40301, "账号已被封禁"),
    PHONE_ALREADY_BOUND(40905, "该手机号已绑定其他账号"),
    POINTS_DEPOSIT_DISABLED(40906, "积分由管理员发放，不支持用户自助存入"),
    POINTS_PAYMENT_DISABLED(40907, "积分仅用于荣誉展示，不支持直接支付"),
    ONLINE_REFUND_DISABLED(40908, "线上退款已停用，请线下人工处理"),
    COUPON_STATE_INVALID(40909, "卡券状态不允许该操作"),
    NOT_FOUND(40400, "数据不存在"),
    IDEMPOTENT_CONFLICT(40901, "请求正在处理或已处理"),
    ORDER_STATE_INVALID(40902, "订单状态不允许该操作"),
    STOCK_NOT_ENOUGH(40903, "商品库存不足"),
    PRICE_CHANGED(40904, "商品价格已变化，请重新确认"),
    BALANCE_NOT_ENOUGH(50101, "余额不足"),
    POINTS_NOT_ENOUGH(50102, "可用积分不足"),
    ACCOUNT_CONCURRENT_ERROR(50103, "账户繁忙，请稍后重试"),
    PAYMENT_VERIFY_FAILED(50201, "支付通知验签失败"),
    PAYMENT_AMOUNT_MISMATCH(50202, "支付金额校验失败"),
    PAYMENT_OWNER_MISMATCH(50203, "支付用户校验失败"),
    FILE_TYPE_INVALID(50301, "仅支持 jpg、jpeg、png 图片"),
    FILE_TOO_LARGE(50302, "文件大小不能超过 5MB"),
    WECHAT_CONFIG_MISSING(50303, "微信小程序配置不完整"),
    WECHAT_API_ERROR(50304, "微信服务调用失败，请稍后重试"),
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
