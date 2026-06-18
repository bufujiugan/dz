package com.dz.tavern.admin.controller;

import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.service.AdminAuthService;
import com.dz.tavern.service.dto.AdminLoginRequest;
import com.dz.tavern.service.dto.AdminLoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(adminAuthService.login(request.username(), request.password()));
    }
}
