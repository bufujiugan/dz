package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_template")
public class CouponTemplateEntity extends BaseEntity {
    private Long storeId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String imageUrl;
    private String description;
    private Long saleProductId;
    private Integer purchaseValidDays;
    private Integer giftValidDays;
    private Integer status;
}
