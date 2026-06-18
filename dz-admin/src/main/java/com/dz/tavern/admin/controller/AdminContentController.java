package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.dao.entity.ActivityEntity;
import com.dz.tavern.dao.entity.SystemConfigEntity;
import com.dz.tavern.dao.entity.StoreOperationConfigEntity;
import com.dz.tavern.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/content")
@RequirePermission("content:manage")
@RequiredArgsConstructor
public class AdminContentController {
    private final ContentService contentService;

    @GetMapping("/activities")
    public ApiResponse<List<ActivityEntity>> activities(
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(contentService.listActivities(storeId, false));
    }

    @PostMapping("/activity")
    @OpLog(module = "ACTIVITY", action = "CREATE")
    public ApiResponse<Long> createActivity(@RequestBody ActivityEntity activity) {
        activity.setId(null);
        return ApiResponse.ok(contentService.saveActivity(activity));
    }

    @PutMapping("/activity")
    @OpLog(module = "ACTIVITY", action = "UPDATE")
    public ApiResponse<Long> updateActivity(@RequestBody ActivityEntity activity) {
        return ApiResponse.ok(contentService.saveActivity(activity));
    }

    @PostMapping("/activity/status")
    @OpLog(module = "ACTIVITY", action = "CHANGE_STATUS")
    public ApiResponse<Void> changeActivityStatus(
            @RequestParam Long id, @RequestParam Integer status) {
        contentService.changeActivityStatus(id, status);
        return ApiResponse.ok();
    }

    @GetMapping("/configs")
    public ApiResponse<List<SystemConfigEntity>> configs() {
        return ApiResponse.ok(contentService.listConfigs());
    }

    @PutMapping("/configs")
    @OpLog(module = "SYSTEM_CONFIG", action = "UPDATE")
    public ApiResponse<Void> updateConfigs(@RequestBody Map<String, String> configs) {
        contentService.updateConfigs(configs);
        return ApiResponse.ok();
    }

    @GetMapping("/store-config")
    public ApiResponse<StoreOperationConfigEntity> storeConfig(@RequestParam Long storeId) {
        return ApiResponse.ok(contentService.getStoreConfig(storeId));
    }

    @PutMapping("/store-config")
    @OpLog(module = "STORE_CONFIG", action = "UPDATE")
    public ApiResponse<Void> updateStoreConfig(
            @RequestBody StoreOperationConfigEntity config) {
        contentService.updateStoreConfig(config);
        return ApiResponse.ok();
    }
}
