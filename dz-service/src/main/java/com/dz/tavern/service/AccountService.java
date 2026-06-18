package com.dz.tavern.service;

import com.dz.tavern.common.enums.AccountChangeType;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.AccountLogEntity;
import com.dz.tavern.dao.entity.UserAccountEntity;

import java.time.YearMonth;

public interface AccountService {
    UserAccountEntity getAccount(Long userId);

    void changeBalance(Long userId, AccountChangeType changeType, long changeValue,
                       String bizNo, String operator, String remark);

    void changePoints(Long userId, AccountChangeType changeType, long changeValue,
                      String bizNo, String operator, String remark);

    void deductBalance(Long userId, long amountFen, String orderNo, String operator);

    void freezePoints(Long userId, long points, String bizNo, String operator);

    void confirmDeductFrozen(Long userId, long points, String bizNo, String operator);

    void unfreezePoints(Long userId, long points, String bizNo, String operator);

    default PageResult<AccountLogEntity> pageLogs(Long userId, String changeType, YearMonth month,
                                                  String bizNo, long current, long size) {
        return pageLogs(null, userId, changeType, month, bizNo, current, size);
    }

    PageResult<AccountLogEntity> pageLogs(Long storeId, Long userId, String changeType,
                                          YearMonth month, String bizNo, long current, long size);
}
