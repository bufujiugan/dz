package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_points_request")
public class StorePointsRequestEntity extends BaseEntity {
    private Long storeId;
    private Long userId;
    private String type;
    private Long points;
    private String remark;
    private String status;
    private Long auditorId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private Long beforePoints;
    private Long afterPoints;
    private String nickname;
    private String phone;
    private Integer userStatus;
}
