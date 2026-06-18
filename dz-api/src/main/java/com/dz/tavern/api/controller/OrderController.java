package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.OrderEntity;
import com.dz.tavern.service.OrderService;
import com.dz.tavern.service.dto.OrderCreateCommand;
import com.dz.tavern.service.dto.OrderCreateResult;
import com.dz.tavern.service.dto.OrderDetail;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/create")
    public ApiResponse<OrderCreateResult> create(
            @Valid @RequestBody OrderCreateCommand command) {
        return ApiResponse.ok(orderService.create(LoginContext.currentId(), command));
    }

    @PostMapping("/cancel/{orderNo}")
    public ApiResponse<Void> cancel(
            @PathVariable String orderNo,
            @RequestParam(required = false) Long storeId) {
        orderService.cancel(LoginContext.currentId(), storeId, orderNo);
        return ApiResponse.ok();
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<OrderEntity>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(orderService.page(
                LoginContext.currentId(), storeId, status, current, size));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetail> detail(
            @PathVariable String orderNo,
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(orderService.detail(LoginContext.currentId(), storeId, orderNo));
    }
}
