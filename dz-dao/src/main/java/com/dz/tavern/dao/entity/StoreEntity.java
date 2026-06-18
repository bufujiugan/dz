package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store")
public class StoreEntity extends BaseEntity {
    private String name;
    private String address;
    private String phone;
    private Integer status;
}
