package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.service.StatisticsService;
import com.dz.tavern.service.dto.SalesStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin-api/statistics")
@RequirePermission("statistics:read")
@RequiredArgsConstructor
public class AdminStatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping("/sales")
    public ApiResponse<SalesStatistics> sales(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(statisticsService.sales(storeId, startDate, endDate));
    }
}
