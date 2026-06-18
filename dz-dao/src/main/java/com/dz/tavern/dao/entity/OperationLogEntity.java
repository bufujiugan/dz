package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLogEntity extends BaseEntity {
    private Long adminId;
    private String module;
    private String action;
    private String paramsDigest;
    private String ip;
    private Long costMs;
}
