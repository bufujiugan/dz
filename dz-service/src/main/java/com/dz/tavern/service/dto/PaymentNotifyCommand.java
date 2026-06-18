package com.dz.tavern.service.dto;

public record PaymentNotifyCommand(String orderNo, String transactionId, Long amountFen,
                                   String openid, String rawBody, boolean signatureValid) {
}
