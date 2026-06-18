package com.dz.tavern.dao.projection;

import lombok.Data;

@Data
public class ProductSalesRow {
    private Long skuId;
    private String productName;
    private String specName;
    private Long quantity;
    private Long salesFen;
}
