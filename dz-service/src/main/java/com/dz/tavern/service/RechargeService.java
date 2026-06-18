package com.dz.tavern.service;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.RechargeOrderEntity;
import com.dz.tavern.dao.entity.RechargeTierEntity;

import java.util.List;

public interface RechargeService {
    List<RechargeTierEntity> listTiers();

    RechargeOrderEntity create(Long userId, Long tierId);

    void credit(String rechargeNo, String operator);

    PageResult<RechargeOrderEntity> page(Long userId, long current, long size);

    void manualCredit(String rechargeNo, Long adminId);

    default PageResult<RechargeOrderEntity> adminPage(String rechargeNo, String status,
                                                      long current, long size) {
        return adminPage(null, rechargeNo, status, current, size);
    }

    PageResult<RechargeOrderEntity> adminPage(Long storeId, String rechargeNo, String status,
                                              long current, long size);

    Long saveTier(RechargeTierEntity tier);

    void deleteTier(Long tierId);
}
