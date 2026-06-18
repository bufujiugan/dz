package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.OperationLogEntity;
import com.dz.tavern.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin-api/operation")
@RequirePermission("operation:read")
@RequiredArgsConstructor
public class AdminOperationController {
    private final OperationLogService operationLogService;

    @GetMapping("/page")
    public ApiResponse<PageResult<OperationLogEntity>> page(
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(operationLogService.page(
                adminId, module, startTime, endTime, current, size));
    }
}
