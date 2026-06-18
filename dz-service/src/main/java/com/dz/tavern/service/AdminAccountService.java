package com.dz.tavern.service;

import com.dz.tavern.service.dto.AccountAdjustCommand;

public interface AdminAccountService {
    void adjust(AccountAdjustCommand command, Long adminId);

    default String exportCsv(Long userId, String changeType, String bizNo) {
        return exportCsv(null, userId, changeType, bizNo);
    }

    String exportCsv(Long storeId, Long userId, String changeType, String bizNo);
}
