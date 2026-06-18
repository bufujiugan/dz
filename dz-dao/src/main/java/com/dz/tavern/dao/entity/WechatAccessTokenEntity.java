package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wechat_access_token")
public class WechatAccessTokenEntity extends BaseEntity {
    private String tokenType;
    private String accessToken;
    private LocalDateTime expireTime;
    private Integer version;
}
