package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_sku")
public class ProductSkuEntity extends BaseEntity {
    private Long productId;
    private String specName;
    private Long priceFen;
    private Integer stock;
    private Integer sales;
}
