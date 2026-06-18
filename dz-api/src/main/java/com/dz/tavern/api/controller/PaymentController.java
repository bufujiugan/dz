package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.service.OrderService;
import com.dz.tavern.service.PaymentService;
import com.dz.tavern.service.dto.PaymentNotifyCommand;
import com.dz.tavern.service.dto.PrepayResult;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    private final OrderService orderService;

    @PostMapping("/prepay")
    public ApiResponse<PrepayResult> prepay(@RequestBody PrepayRequest request) {
        return ApiResponse.ok(paymentService.prepay(
                LoginContext.currentId(), request.storeId(), request.orderNo()));
    }

    @PostMapping("/notify/wechat")
    public ResponseEntity<Map<String, String>> notifyWechat(
            @RequestHeader(name = "X-Mock-Signature", required = false) String signature,
            @RequestBody MockNotifyRequest request) {
        try {
            paymentService.handleNotify(new PaymentNotifyCommand(
                    request.orderNo(), request.transactionId(), request.amountFen(),
                    request.openid(), request.toString(), "valid".equals(signature)));
            return ResponseEntity.ok(Map.of("code", "SUCCESS"));
        } catch (BizException exception) {
            log.warn("支付回调处理失败 orderNo={} transactionId={}",
                    request.orderNo(), request.transactionId(), exception);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "FAIL", "message", exception.getMessage()));
        }
    }

    @PostMapping("/notify/refund")
    public ResponseEntity<Map<String, String>> notifyRefund(
            @RequestHeader(name = "X-Mock-Signature", required = false) String signature,
            @RequestBody RefundNotifyRequest request) {
        if (!"valid".equals(signature)) {
            log.warn("退款回调验签失败 orderNo={}", request.orderNo());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "FAIL"));
        }
        log.info("收到退款成功回调 orderNo={}", request.orderNo());
        orderService.completeRefund(request.orderNo());
        log.info("退款回调处理完成 orderNo={}", request.orderNo());
        return ResponseEntity.ok(Map.of("code", "SUCCESS"));
    }

    public record PrepayRequest(@NotBlank String orderNo, Long storeId) {
    }

    public record MockNotifyRequest(String orderNo, String transactionId,
                                    Long amountFen, String openid) {
    }

    public record RefundNotifyRequest(String orderNo) {
    }
}
