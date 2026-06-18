package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.RechargeOrderEntity;
import com.dz.tavern.dao.entity.RechargeTierEntity;
import com.dz.tavern.service.RechargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/recharge")
@RequirePermission("recharge:manage")
@RequiredArgsConstructor
public class AdminRechargeController {
    private final RechargeService rechargeService;

    @GetMapping("/tiers")
    public ApiResponse<List<RechargeTierEntity>> tiers() {
        return ApiResponse.ok(rechargeService.listTiers());
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<RechargeOrderEntity>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String rechargeNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(rechargeService.adminPage(
                storeId, rechargeNo, status, current, size));
    }

    @PostMapping("/tier")
    @OpLog(module = "RECHARGE_TIER", action = "CREATE")
    public ApiResponse<Long> createTier(@RequestBody RechargeTierEntity tier) {
        tier.setId(null);
        return ApiResponse.ok(rechargeService.saveTier(tier));
    }

    @PutMapping("/tier")
    @OpLog(module = "RECHARGE_TIER", action = "UPDATE")
    public ApiResponse<Long> updateTier(@RequestBody RechargeTierEntity tier) {
        return ApiResponse.ok(rechargeService.saveTier(tier));
    }

    @DeleteMapping("/tier/{id}")
    @OpLog(module = "RECHARGE_TIER", action = "DELETE")
    public ApiResponse<Void> deleteTier(@PathVariable Long id) {
        rechargeService.deleteTier(id);
        return ApiResponse.ok();
    }

    @PostMapping("/manual-credit/{rechargeNo}")
    @OpLog(module = "RECHARGE", action = "MANUAL_CREDIT")
    public ApiResponse<Void> manualCredit(@PathVariable String rechargeNo) {
        rechargeService.manualCredit(rechargeNo, LoginContext.currentId());
        return ApiResponse.ok();
    }
}
