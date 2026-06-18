package com.dz.tavern.dao.projection;

import lombok.Data;

@Data
public class SalesSummaryRow {
    private Long salesFen;
    private Long orderCount;
    private Long itemQuantity;
}
