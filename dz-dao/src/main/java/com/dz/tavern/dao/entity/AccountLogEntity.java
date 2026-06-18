package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("account_log")
public class AccountLogEntity extends BaseEntity {
    private Long userId;
    private String assetType;
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
