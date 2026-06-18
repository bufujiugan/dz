package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_user")
public class AdminUserEntity extends BaseEntity {
    private String username;
    private String password;
    private String realName;
    private Long roleId;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private Integer loginFailCount;
    private LocalDateTime lockedUntil;
}
