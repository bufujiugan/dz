package com.dz.tavern.service;

import com.dz.tavern.service.dto.SalesStatistics;

import java.time.LocalDate;

public interface StatisticsService {
    SalesStatistics sales(Long storeId, LocalDate startDate, LocalDate endDate);
}
