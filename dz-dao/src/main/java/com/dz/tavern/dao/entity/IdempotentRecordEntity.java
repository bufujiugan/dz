package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("idempotent_record")
public class IdempotentRecordEntity extends BaseEntity {
    private String idempotentKey;
    private String status;
    private LocalDateTime expireTime;
}
