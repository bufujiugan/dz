package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_operation_config")
public class StoreOperationConfigEntity extends BaseEntity {
    private Long storeId;
    private String businessEndTime;
    private String homeSlogan;
    private String heroImage;
    private String gameplayDescription;
    private String menuTitle;
    private Integer pointsHalvingEnabled;
    private Integer pointsHalvingDay;
    private String pointsHalvingTime;
    private String pointsHalvingLastPeriod;
}
