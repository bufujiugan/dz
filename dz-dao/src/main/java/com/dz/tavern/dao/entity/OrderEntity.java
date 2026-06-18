package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order")
public class OrderEntity extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Long storeId;
    private Long totalFen;
    private String payType;
    private String status;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private String remark;
    private Integer prepayRequested;
    private String nickname;
    private String phone;
}
