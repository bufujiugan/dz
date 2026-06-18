package com.dz.tavern.service;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.StorePointsLogEntity;
import com.dz.tavern.dao.entity.StorePointsRequestEntity;
import com.dz.tavern.dao.entity.UserStorePointsEntity;
import com.dz.tavern.dao.projection.StorePointsUserSummaryRow;
import com.dz.tavern.service.dto.StoreLeaderboardView;
import com.dz.tavern.service.dto.StorePointsAdjustCommand;
import com.dz.tavern.service.dto.StorePointsApplyCommand;
import com.dz.tavern.service.dto.StorePointsAuditCommand;
import com.dz.tavern.service.dto.StorePointsSetCommand;

public interface StorePointsService {
    UserStorePointsEntity getAccount(Long userId, Long storeId);

    StoreLeaderboardView leaderboard(Long userId, Long storeId);

    Long deposit(Long userId, StorePointsApplyCommand command);

    Long withdraw(Long userId, StorePointsApplyCommand command);

    PageResult<StorePointsRequestEntity> pageRequests(
            Long storeId, Long userId, String status, long current, long size);

    PageResult<StorePointsUserSummaryRow> pageUserSummaries(
            Long storeId, String keyword, long current, long size);

    void audit(StorePointsAuditCommand command, Long adminId);

    void adjust(StorePointsAdjustCommand command, Long adminId);

    void setPoints(StorePointsSetCommand command, Long adminId);

    PageResult<StorePointsLogEntity> pageLogs(
            Long storeId, Long userId, long current, long size);

    void halveStorePoints(Long storeId, String period);
}
