package com.dz.tavern.service.impl;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.dao.mapper.StatisticsMapper;
import com.dz.tavern.dao.projection.SalesSummaryRow;
import com.dz.tavern.service.StatisticsService;
import com.dz.tavern.service.dto.SalesStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsMapper statisticsMapper;

    @Override
    public SalesStatistics sales(Long storeId, LocalDate startDate, LocalDate endDate) {
        LocalDate normalizedEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate normalizedStart = startDate == null
                ? normalizedEnd.minusDays(29) : startDate;
        if (normalizedStart.isAfter(normalizedEnd)
                || normalizedStart.plusYears(1).isBefore(normalizedEnd)) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        LocalDateTime startTime = normalizedStart.atStartOfDay();
        LocalDateTime endTime = normalizedEnd.plusDays(1).atStartOfDay();
        SalesSummaryRow summary = statisticsMapper.selectSummary(storeId, startTime, endTime);
        return new SalesStatistics(
                normalizedStart,
                normalizedEnd,
                summary == null ? 0L : summary.getSalesFen(),
                summary == null ? 0L : summary.getOrderCount(),
                summary == null ? 0L : summary.getItemQuantity(),
                statisticsMapper.selectDailySales(storeId, startTime, endTime),
                statisticsMapper.selectProductSales(storeId, startTime, endTime, 100));
    }
}
