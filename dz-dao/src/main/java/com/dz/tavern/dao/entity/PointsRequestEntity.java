package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("points_request")
public class PointsRequestEntity extends BaseEntity {
    private String type;
    private Long userId;
    private Long points;
    private String remark;
    private String voucherImages;
    private String status;
    private Long auditorId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private Long beforePoints;
    private Long afterPoints;
    private String nickname;
    private String phone;
}
