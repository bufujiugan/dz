package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.StorePointsLogEntity;
import com.dz.tavern.dao.entity.StorePointsRequestEntity;
import com.dz.tavern.dao.entity.UserStorePointsEntity;
import com.dz.tavern.service.StorePointsService;
import com.dz.tavern.service.dto.StoreLeaderboardView;
import com.dz.tavern.service.dto.StorePointsApplyCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store-points")
@RequiredArgsConstructor
public class StorePointsController {
    private final StorePointsService storePointsService;

    @GetMapping("/account")
    public ApiResponse<UserStorePointsEntity> account(@RequestParam Long storeId) {
        return ApiResponse.ok(storePointsService.getAccount(
                LoginContext.currentId(), storeId));
    }

    @GetMapping("/leaderboard")
    public ApiResponse<StoreLeaderboardView> leaderboard(@RequestParam Long storeId) {
        return ApiResponse.ok(storePointsService.leaderboard(
                LoginContext.currentId(), storeId));
    }

    @PostMapping("/deposit")
    public ApiResponse<Long> deposit(
            @Valid @RequestBody StorePointsApplyCommand command) {
        return ApiResponse.ok(storePointsService.deposit(
                LoginContext.currentId(), command));
    }

    @PostMapping("/withdraw")
    public ApiResponse<Long> withdraw(
            @Valid @RequestBody StorePointsApplyCommand command) {
        return ApiResponse.ok(storePointsService.withdraw(
                LoginContext.currentId(), command));
    }

    @GetMapping("/requests")
    public ApiResponse<PageResult<StorePointsRequestEntity>> requests(
            @RequestParam Long storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(storePointsService.pageRequests(
                storeId, LoginContext.currentId(), status, current, size));
    }

    @GetMapping("/logs")
    public ApiResponse<PageResult<StorePointsLogEntity>> logs(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(storePointsService.pageLogs(
                storeId, LoginContext.currentId(), current, size));
    }
}
