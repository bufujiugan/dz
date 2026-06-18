package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.PaymentRecordEntity;
import com.dz.tavern.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/pay")
@RequirePermission("pay:manage")
@RequiredArgsConstructor
public class AdminPaymentController {
    private final PaymentService paymentService;

    @GetMapping("/page")
    public ApiResponse<PageResult<PaymentRecordEntity>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String tradeState,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(paymentService.pageRecords(
                storeId, orderNo, tradeState, current, size));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<PaymentRecordEntity> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(paymentService.getRecord(orderNo));
    }
}
