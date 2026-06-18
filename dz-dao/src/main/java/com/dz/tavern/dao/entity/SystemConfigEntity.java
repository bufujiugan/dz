package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfigEntity extends BaseEntity {
    private String configKey;
    private String configValue;
    private String description;
}
