package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_redeem_request")
public class CouponRedeemRequestEntity extends BaseEntity {
    private Long userCouponId;
    private Long userId;
    private String status;
    private String remark;
    private Long auditorId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private Long storeId;
    private String couponNo;
    private String couponName;
    private String nickname;
    private String phone;
}
