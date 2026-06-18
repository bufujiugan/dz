package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_record")
public class PaymentRecordEntity extends BaseEntity {
    private String orderNo;
    private String transactionId;
    private Long amountFen;
    private String openid;
    private String tradeState;
    private String notifyRaw;
    private String verifyResult;
    private String queryResult;
    private Integer notifyCount;
    private Long userId;
    private Long storeId;
    private String nickname;
    private String phone;
}
