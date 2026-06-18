package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.dao.entity.CouponCategoryEntity;
import com.dz.tavern.dao.entity.CouponTemplateEntity;
import com.dz.tavern.dao.entity.UserCouponEntity;
import com.dz.tavern.service.CouponService;
import com.dz.tavern.service.dto.CouponRedeemCommand;
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
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @GetMapping("/categories")
    public ApiResponse<List<CouponCategoryEntity>> categories(@RequestParam Long storeId) {
        return ApiResponse.ok(couponService.listCategories(storeId, true));
    }

    @GetMapping("/templates")
    public ApiResponse<List<CouponTemplateEntity>> templates(@RequestParam Long storeId) {
        return ApiResponse.ok(couponService.listTemplates(storeId, true));
    }

    @GetMapping("/mine")
    public ApiResponse<List<UserCouponEntity>> mine(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(couponService.listUserCoupons(
                LoginContext.currentId(), storeId, status));
    }

    @PostMapping("/redeem")
    public ApiResponse<Long> redeem(@Valid @RequestBody CouponRedeemCommand command) {
        return ApiResponse.ok(couponService.requestRedeem(
                LoginContext.currentId(), command));
    }
}
