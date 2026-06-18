package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_store_points")
public class UserStorePointsEntity extends BaseEntity {
    private Long userId;
    private Long storeId;
    private Long points;
    private Long frozenPoints;
    private Integer version;
}
