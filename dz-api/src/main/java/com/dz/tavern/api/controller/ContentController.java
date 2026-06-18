package com.dz.tavern.api.controller;

import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.dao.entity.ActivityEntity;
import com.dz.tavern.service.ContentService;
import com.dz.tavern.service.dto.PublicContentView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {
    private final ContentService contentService;

    @GetMapping("/config")
    public ApiResponse<PublicContentView> config(
            @org.springframework.web.bind.annotation.RequestParam Long storeId) {
        return ApiResponse.ok(contentService.getPublicContent(storeId));
    }

    @GetMapping("/activities")
    public ApiResponse<List<ActivityEntity>> activities(
            @org.springframework.web.bind.annotation.RequestParam Long storeId) {
        return ApiResponse.ok(contentService.listActivities(storeId, true));
    }
}
