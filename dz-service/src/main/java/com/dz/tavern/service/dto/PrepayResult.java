package com.dz.tavern.service.dto;

public record PrepayResult(String timeStamp, String nonceStr, String packageValue,
                           String signType, String paySign) {
}
