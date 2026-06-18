package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class UserEntity extends BaseEntity {
    private Long storeId;
    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer status;
}
