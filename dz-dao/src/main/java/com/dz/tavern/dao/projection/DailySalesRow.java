package com.dz.tavern.dao.projection;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailySalesRow {
    private LocalDate saleDate;
    private Long salesFen;
    private Long orderCount;
}
