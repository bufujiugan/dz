package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_category")
public class CouponCategoryEntity extends BaseEntity {
    private Long storeId;
    private String name;
    private Integer sort;
    private Integer status;
}
