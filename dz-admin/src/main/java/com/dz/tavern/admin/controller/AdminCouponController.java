package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.CouponCategoryEntity;
import com.dz.tavern.dao.entity.CouponRedeemRequestEntity;
import com.dz.tavern.dao.entity.CouponTemplateEntity;
import com.dz.tavern.dao.entity.UserCouponEntity;
import com.dz.tavern.service.CouponService;
import com.dz.tavern.service.dto.CouponAuditCommand;
import com.dz.tavern.service.dto.CouponGiftCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/coupon")
@RequirePermission("coupon:manage")
@RequiredArgsConstructor
public class AdminCouponController {
    private final CouponService couponService;

    @GetMapping("/categories")
    public ApiResponse<List<CouponCategoryEntity>> categories(
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(couponService.listCategories(storeId, false));
    }

    @GetMapping("/templates")
    public ApiResponse<List<CouponTemplateEntity>> templates(
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(couponService.listTemplates(storeId, false));
    }

    @PostMapping("/template")
    @OpLog(module = "COUPON", action = "CREATE_TEMPLATE")
    public ApiResponse<Long> createTemplate(@RequestBody CouponTemplateEntity template) {
        template.setId(null);
        return ApiResponse.ok(couponService.saveTemplate(template));
    }

    @PutMapping("/template")
    @OpLog(module = "COUPON", action = "UPDATE_TEMPLATE")
    public ApiResponse<Long> updateTemplate(@RequestBody CouponTemplateEntity template) {
        return ApiResponse.ok(couponService.saveTemplate(template));
    }

    @PostMapping("/template/status")
    @OpLog(module = "COUPON", action = "CHANGE_TEMPLATE_STATUS")
    public ApiResponse<Void> changeTemplateStatus(
            @RequestParam Long id, @RequestParam Integer status) {
        couponService.changeTemplateStatus(id, status);
        return ApiResponse.ok();
    }

    @PostMapping("/gift")
    @OpLog(module = "COUPON", action = "GIFT")
    public ApiResponse<Void> gift(@Valid @RequestBody CouponGiftCommand command) {
        couponService.gift(command, LoginContext.currentId());
        return ApiResponse.ok();
    }

    @GetMapping("/user-page")
    public ApiResponse<PageResult<UserCouponEntity>> userCoupons(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(couponService.adminPageCoupons(
                storeId, userId, status, current, size));
    }

    @GetMapping("/redeem-page")
    public ApiResponse<PageResult<CouponRedeemRequestEntity>> redeemRequests(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(couponService.pageRedeemRequests(
                storeId, status, current, size));
    }

    @PostMapping("/redeem-audit")
    @OpLog(module = "COUPON", action = "REDEEM_AUDIT")
    public ApiResponse<Void> auditRedeem(@Valid @RequestBody CouponAuditCommand command) {
        couponService.auditRedeem(command, LoginContext.currentId());
        return ApiResponse.ok();
    }
}
