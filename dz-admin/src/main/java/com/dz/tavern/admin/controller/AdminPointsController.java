package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.PointsRequestEntity;
import com.dz.tavern.service.PointsService;
import com.dz.tavern.service.dto.PointsAuditCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/points")
@RequirePermission("points:audit")
@RequiredArgsConstructor
public class AdminPointsController {
    private final PointsService pointsService;

    @GetMapping("/page")
    public ApiResponse<PageResult<PointsRequestEntity>> page(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(pointsService.page(storeId, userId, type, status, current, size));
    }

    @PostMapping("/audit")
    @OpLog(module = "POINTS", action = "AUDIT")
    public ApiResponse<Void> audit(@Valid @RequestBody PointsAuditCommand command) {
        pointsService.audit(command, LoginContext.currentId());
        return ApiResponse.ok();
    }
}
