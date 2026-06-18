package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.RechargeOrderEntity;
import com.dz.tavern.dao.entity.RechargeTierEntity;
import com.dz.tavern.service.PaymentService;
import com.dz.tavern.service.RechargeService;
import com.dz.tavern.service.dto.PrepayResult;
import com.dz.tavern.service.dto.RechargeCreateCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recharge")
@RequiredArgsConstructor
public class RechargeController {
    private final RechargeService rechargeService;
    private final PaymentService paymentService;

    @GetMapping("/tiers")
    public ApiResponse<List<RechargeTierEntity>> tiers() {
        return ApiResponse.ok(rechargeService.listTiers());
    }

    @PostMapping("/create")
    public ApiResponse<RechargeCreateResult> create(
            @Valid @RequestBody RechargeCreateCommand command) {
        Long userId = LoginContext.currentId();
        RechargeOrderEntity order = rechargeService.create(userId, command.tierId());
        PrepayResult prepay = paymentService.prepayRecharge(userId, order.getRechargeNo());
        return ApiResponse.ok(new RechargeCreateResult(order, prepay));
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<RechargeOrderEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(rechargeService.page(
                LoginContext.currentId(), current, size));
    }

    public record RechargeCreateResult(RechargeOrderEntity order, PrepayResult prepay) {
    }
}
