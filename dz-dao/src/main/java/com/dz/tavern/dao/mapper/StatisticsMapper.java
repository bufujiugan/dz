package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.projection.DailySalesRow;
import com.dz.tavern.dao.projection.ProductSalesRow;
import com.dz.tavern.dao.projection.SalesSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StatisticsMapper {
    SalesSummaryRow selectSummary(@Param("storeId") Long storeId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    List<DailySalesRow> selectDailySales(@Param("storeId") Long storeId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    List<ProductSalesRow> selectProductSales(@Param("storeId") Long storeId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("limit") int limit);
}
