package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.service.MockPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wechat.mock-enabled", havingValue = "true")
public class MockPaymentController {
    private final MockPaymentService mockPaymentService;

    @PostMapping("/mock-success")
    public ApiResponse<Void> mockSuccess(@Valid @RequestBody MockSuccessRequest request) {
        mockPaymentService.simulateSuccess(LoginContext.currentId(), request.bizNo());
        return ApiResponse.ok();
    }

    public record MockSuccessRequest(@NotBlank String bizNo) {
    }
}
