package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recharge_order")
public class RechargeOrderEntity extends BaseEntity {
    private String rechargeNo;
    private Long userId;
    private Long tierId;
    private Long payFen;
    private Long bonusFen;
    private String status;
    private Integer credited;
    private Integer prepayRequested;
    private String nickname;
    private String phone;
}
