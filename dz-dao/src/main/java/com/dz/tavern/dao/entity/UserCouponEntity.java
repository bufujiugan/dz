package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_coupon")
public class UserCouponEntity extends BaseEntity {
    private String couponNo;
    private Long templateId;
    private Long userId;
    private Long storeId;
    private String couponName;
    private String imageUrl;
    private String sourceType;
    private String sourceNo;
    private String status;
    private LocalDateTime expireTime;
    private LocalDateTime usedTime;
    private String nickname;
    private String phone;
}
