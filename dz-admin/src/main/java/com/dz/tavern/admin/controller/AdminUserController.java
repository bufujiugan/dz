package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.UserEntity;
import com.dz.tavern.service.UserService;
import com.dz.tavern.service.dto.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/user")
@RequirePermission("user:manage")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping("/page")
    public ApiResponse<PageResult<UserEntity>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(userService.pageUsers(storeId, keyword, current, size));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfile> detail(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PostMapping("/{userId}/status")
    @OpLog(module = "USER", action = "CHANGE_STATUS")
    public ApiResponse<Void> changeStatus(@PathVariable Long userId,
                                          @RequestParam Integer status) {
        userService.changeStatus(userId, status);
        return ApiResponse.ok();
    }
}
