package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity")
public class ActivityEntity extends BaseEntity {
    private Long storeId;
    private String title;
    private String imageUrl;
    private Integer sort;
    private Integer status;
}
