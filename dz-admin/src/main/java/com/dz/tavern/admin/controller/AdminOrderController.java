package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.OrderEntity;
import com.dz.tavern.service.OrderService;
import com.dz.tavern.service.PaymentService;
import com.dz.tavern.service.dto.OrderDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/order")
@RequirePermission("order:manage")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService orderService;
    private final PaymentService paymentService;

    @GetMapping("/page")
    public ApiResponse<PageResult<OrderEntity>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(orderService.adminPage(storeId, orderNo, status, current, size));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetail> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.detail(null, orderNo));
    }

    @PostMapping("/{orderNo}/complete")
    @OpLog(module = "ORDER", action = "COMPLETE")
    public ApiResponse<Void> complete(@PathVariable String orderNo) {
        orderService.complete(orderNo);
        return ApiResponse.ok();
    }

    @PostMapping("/{orderNo}/refund")
    @OpLog(module = "ORDER", action = "REFUND")
    public ApiResponse<Void> refund(@PathVariable String orderNo) {
        paymentService.refund(orderNo, LoginContext.currentId());
        return ApiResponse.ok();
    }
}
