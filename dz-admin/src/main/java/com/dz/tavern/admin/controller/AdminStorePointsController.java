package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.StorePointsLogEntity;
import com.dz.tavern.dao.entity.StorePointsRequestEntity;
import com.dz.tavern.dao.projection.StorePointsUserSummaryRow;
import com.dz.tavern.service.StorePointsService;
import com.dz.tavern.service.dto.StorePointsAdjustCommand;
import com.dz.tavern.service.dto.StorePointsAuditCommand;
import com.dz.tavern.service.dto.StorePointsSetCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/store-points")
@RequirePermission("points:audit")
@RequiredArgsConstructor
public class AdminStorePointsController {
    private final StorePointsService storePointsService;

    @GetMapping("/requests")
    public ApiResponse<PageResult<StorePointsRequestEntity>> requests(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(storePointsService.pageRequests(
                storeId, userId, status, current, size));
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<StorePointsUserSummaryRow>> users(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(storePointsService.pageUserSummaries(
                storeId, keyword, current, size));
    }

    @PostMapping("/audit")
    @OpLog(module = "STORE_POINTS", action = "AUDIT")
    public ApiResponse<Void> audit(
            @Valid @RequestBody StorePointsAuditCommand command) {
        storePointsService.audit(command, LoginContext.currentId());
        return ApiResponse.ok();
    }

    @PostMapping("/adjust")
    @OpLog(module = "STORE_POINTS", action = "ADJUST")
    public ApiResponse<Void> adjust(
            @Valid @RequestBody StorePointsAdjustCommand command) {
        storePointsService.adjust(command, LoginContext.currentId());
        return ApiResponse.ok();
    }

    @PostMapping("/set")
    @OpLog(module = "STORE_POINTS", action = "SET")
    public ApiResponse<Void> setPoints(
            @Valid @RequestBody StorePointsSetCommand command) {
        storePointsService.setPoints(command, LoginContext.currentId());
        return ApiResponse.ok();
    }

    @GetMapping("/logs")
    public ApiResponse<PageResult<StorePointsLogEntity>> logs(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(storePointsService.pageLogs(
                storeId, userId, current, size));
    }
}
