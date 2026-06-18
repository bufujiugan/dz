package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.service.ReconcileService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/reconcile")
@RequirePermission("reconcile:manage")
@RequiredArgsConstructor
public class AdminReconcileController {
    private final ReconcileService reconcileService;

    @PostMapping("/daily")
    @OpLog(module = "RECONCILE", action = "DAILY")
    public ApiResponse<Map<String, Object>> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(reconcileService.reconcileDaily(date));
    }
}
