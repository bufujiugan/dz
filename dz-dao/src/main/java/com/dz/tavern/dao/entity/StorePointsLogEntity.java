package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_points_log")
public class StorePointsLogEntity extends BaseEntity {
    private Long storeId;
    private Long userId;
    private String changeType;
    private Long changeValue;
    private Long beforeValue;
    private Long afterValue;
    private String bizNo;
    private String operator;
    private String remark;
    private String nickname;
    private String phone;
}
