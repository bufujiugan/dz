package com.dz.tavern.service.dto;

import com.dz.tavern.dao.projection.DailySalesRow;
import com.dz.tavern.dao.projection.ProductSalesRow;

import java.time.LocalDate;
import java.util.List;

public record SalesStatistics(
        LocalDate startDate,
        LocalDate endDate,
        Long salesFen,
        Long orderCount,
        Long itemQuantity,
        List<DailySalesRow> dailySales,
        List<ProductSalesRow> productSales) {
}
