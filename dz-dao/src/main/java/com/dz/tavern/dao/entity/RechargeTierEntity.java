package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recharge_tier")
public class RechargeTierEntity extends BaseEntity {
    private Long payFen;
    private Long bonusFen;
    private String label;
    private Integer status;
    private Integer sort;
}
