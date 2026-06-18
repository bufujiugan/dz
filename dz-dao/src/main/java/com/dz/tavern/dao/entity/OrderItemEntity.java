package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItemEntity extends BaseEntity {
    private Long orderId;
    private Long skuId;
    private String productName;
    private String specName;
    private Long priceFen;
    private Integer quantity;
}
