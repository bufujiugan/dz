package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.PointsRequestEntity;
import com.dz.tavern.service.PointsService;
import com.dz.tavern.service.dto.PointsApplyCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {
    private final PointsService pointsService;

    @PostMapping("/deposit")
    public ApiResponse<Long> deposit(@Valid @RequestBody PointsApplyCommand command) {
        return ApiResponse.ok(pointsService.deposit(LoginContext.currentId(), command));
    }

    @PostMapping("/withdraw")
    public ApiResponse<Long> withdraw(@Valid @RequestBody PointsApplyCommand command) {
        return ApiResponse.ok(pointsService.withdraw(LoginContext.currentId(), command));
    }

    @GetMapping("/requests")
    public ApiResponse<PageResult<PointsRequestEntity>> requests(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(pointsService.page(
                LoginContext.currentId(), type, status, current, size));
    }
}
