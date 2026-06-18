package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.service.AuthService;
import com.dz.tavern.service.dto.LoginRequest;
import com.dz.tavern.service.dto.LoginResponse;
import com.dz.tavern.service.dto.MiniProgramAuthorizeRequest;
import com.dz.tavern.service.dto.PhoneBindRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.code(), request.storeId()));
    }

    @PostMapping("/authorize")
    public ApiResponse<LoginResponse> authorizeMiniProgram(
            @Valid @RequestBody MiniProgramAuthorizeRequest request) {
        return ApiResponse.ok(authService.authorizeMiniProgram(request));
    }

    @PostMapping("/phone")
    public ApiResponse<Void> bindPhone(@Valid @RequestBody PhoneBindRequest request) {
        authService.bindPhone(LoginContext.currentId(), request.code());
        return ApiResponse.ok();
    }
}
