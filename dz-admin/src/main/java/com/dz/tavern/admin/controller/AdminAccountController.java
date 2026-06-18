package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.AccountLogEntity;
import com.dz.tavern.service.AccountService;
import com.dz.tavern.service.AdminAccountService;
import com.dz.tavern.service.dto.AccountAdjustCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/admin-api/account")
@RequiredArgsConstructor
public class AdminAccountController {
    private final AccountService accountService;
    private final AdminAccountService adminAccountService;

    @PostMapping("/adjust")
    @RequirePermission("account:adjust")
    @OpLog(module = "ACCOUNT", action = "ADJUST")
    public ApiResponse<Void> adjust(@Valid @RequestBody AccountAdjustCommand command) {
        adminAccountService.adjust(command, LoginContext.currentId());
        return ApiResponse.ok();
    }

    @GetMapping("/logs")
    @RequirePermission("operation:read")
    public ApiResponse<PageResult<AccountLogEntity>> logs(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String bizNo,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(accountService.pageLogs(
                storeId, userId, type, null, bizNo, current, size));
    }

    @GetMapping(value = "/logs/export", produces = "text/csv")
    @RequirePermission("operation:read")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String bizNo) {
        byte[] content = adminAccountService.exportCsv(storeId, userId, type, bizNo)
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=account-logs.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(content);
    }
}
