package com.dz.tavern.service.impl;

import com.dz.tavern.common.annotation.Idempotent;
import com.dz.tavern.common.enums.PointsRequestType;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.StorePointsLogEntity;
import com.dz.tavern.dao.entity.StorePointsRequestEntity;
import com.dz.tavern.dao.entity.UserStorePointsEntity;
import com.dz.tavern.dao.mapper.StorePointsMapper;
import com.dz.tavern.dao.projection.StorePointsRankingRow;
import com.dz.tavern.dao.projection.StorePointsUserSummaryRow;
import com.dz.tavern.service.NotificationService;
import com.dz.tavern.service.StorePointsService;
import com.dz.tavern.service.UserService;
import com.dz.tavern.service.dto.StoreLeaderboardView;
import com.dz.tavern.service.dto.StorePointsAdjustCommand;
import com.dz.tavern.service.dto.StorePointsApplyCommand;
import com.dz.tavern.service.dto.StorePointsAuditCommand;
import com.dz.tavern.service.dto.StorePointsEntry;
import com.dz.tavern.service.dto.StorePointsSetCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorePointsServiceImpl implements StorePointsService {
    private static final int MAX_RETRY_TIMES = 3;

    private final StorePointsMapper storePointsMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public UserStorePointsEntity getAccount(Long userId, Long storeId) {
        if (userId == null || storeId == null) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        UserStorePointsEntity account = storePointsMapper.selectAccount(userId, storeId);
        if (account != null) {
            return account;
        }
        UserStorePointsEntity created = new UserStorePointsEntity();
        created.setUserId(userId);
        created.setStoreId(storeId);
        created.setPoints(0L);
        created.setFrozenPoints(0L);
        created.setVersion(0);
        storePointsMapper.insertAccount(created);
        return storePointsMapper.selectAccount(userId, storeId);
    }

    @Override
    @Transactional
    public StoreLeaderboardView leaderboard(Long userId, Long storeId) {
        getAccount(userId, storeId);
        List<StorePointsEntry> top20 = storePointsMapper.selectRanking(storeId, 20)
                .stream().map(this::toEntry).toList();
        StorePointsRankingRow mine = storePointsMapper.selectUserRanking(storeId, userId);
        return new StoreLeaderboardView(storeId, top20, toEntry(mine));
    }

    @Override
    @Transactional
    public Long deposit(Long userId, StorePointsApplyCommand command) {
        userService.checkUserActive(userId);
        getAccount(userId, command.storeId());
        StorePointsRequestEntity request = createRequest(
                userId, command, PointsRequestType.DEPOSIT);
        log.info("门店存积分申请已创建 requestId={} userId={} storeId={} points={}",
                request.getId(), userId, command.storeId(), command.points());
        return request.getId();
    }

    @Override
    @Transactional
    public Long withdraw(Long userId, StorePointsApplyCommand command) {
        userService.checkUserActive(userId);
        String bizNo = "STORE_POINTS_REQUEST_" + System.currentTimeMillis() + "_" + userId;
        changeAccount(userId, command.storeId(), 0, command.points(), bizNo,
                "USER:" + userId, "提交取积分申请并冻结积分");
        StorePointsRequestEntity request = createRequest(
                userId, command, PointsRequestType.WITHDRAW);
        log.info("门店积分取出申请已创建 requestId={} userId={} storeId={} points={}",
                request.getId(), userId, command.storeId(), command.points());
        return request.getId();
    }

    @Override
    public PageResult<StorePointsRequestEntity> pageRequests(
            Long storeId, Long userId, String status, long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                storePointsMapper.countRequests(storeId, userId, status),
                storePointsMapper.selectRequests(
                        storeId, userId, status, offset, normalizedSize));
    }

    @Override
    public PageResult<StorePointsUserSummaryRow> pageUserSummaries(
            Long storeId, String keyword, long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        return new PageResult<>(normalizedCurrent, normalizedSize,
                storePointsMapper.countUserSummaries(storeId, normalizedKeyword),
                storePointsMapper.selectUserSummaries(
                        storeId, normalizedKeyword, offset, normalizedSize));
    }

    @Override
    @Transactional
    @Idempotent(key = "#command.requestId")
    public void audit(StorePointsAuditCommand command, Long adminId) {
        StorePointsRequestEntity request =
                storePointsMapper.selectRequestById(command.requestId());
        if (request == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        UserStorePointsEntity before = getAccount(request.getUserId(), request.getStoreId());
        String bizNo = "STORE_POINTS_AUDIT_" + request.getId();
        applyAuditResult(request, command.approve(), adminId, bizNo);
        UserStorePointsEntity after = getAccount(request.getUserId(), request.getStoreId());
        request.setStatus(command.approve() ? "APPROVED" : "REJECTED");
        request.setAuditorId(adminId);
        request.setAuditRemark(command.auditRemark());
        request.setAuditTime(LocalDateTime.now());
        request.setBeforePoints(before.getPoints());
        request.setAfterPoints(after.getPoints());
        if (storePointsMapper.updateRequestAudit(request) == 0) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT);
        }
        notificationService.createTask(request.getUserId(), "STORE_POINTS_AUDIT",
                "{\"requestId\":" + request.getId() + ",\"status\":\""
                        + request.getStatus() + "\"}");
        log.info("门店积分申请审核完成 adminId={} requestId={} userId={} storeId={} approve={}",
                adminId, request.getId(), request.getUserId(),
                request.getStoreId(), command.approve());
    }

    @Override
    @Transactional
    public void adjust(StorePointsAdjustCommand command, Long adminId) {
        if (command.value() == 0) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        String bizNo = "STORE_POINTS_ADJUST_" + adminId + "_" + System.currentTimeMillis();
        changeAccount(command.userId(), command.storeId(), command.value(), 0,
                bizNo, "ADMIN:" + adminId,
                command.remark() == null ? "管理员调整门店积分" : command.remark());
        log.info("管理员调整门店积分完成 adminId={} userId={} storeId={} value={} bizNo={}",
                adminId, command.userId(), command.storeId(), command.value(), bizNo);
    }

    @Override
    @Transactional
    public void setPoints(StorePointsSetCommand command, Long adminId) {
        String bizNo = "STORE_POINTS_SET_" + adminId + "_" + System.currentTimeMillis();
        changeAccountToPoints(command.userId(), command.storeId(), command.points(),
                bizNo, "ADMIN:" + adminId,
                command.remark() == null ? "管理员直接设置门店积分" : command.remark());
        log.info("管理员直接设置门店积分完成 adminId={} userId={} storeId={} points={} bizNo={}",
                adminId, command.userId(), command.storeId(), command.points(), bizNo);
    }

    @Override
    public PageResult<StorePointsLogEntity> pageLogs(
            Long storeId, Long userId, long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                storePointsMapper.countLogs(storeId, userId),
                storePointsMapper.selectLogs(storeId, userId, offset, normalizedSize));
    }

    @Override
    @Transactional
    public void halveStorePoints(Long storeId, String period) {
        List<UserStorePointsEntity> accounts =
                storePointsMapper.selectStoreAccountsForUpdate(storeId);
        int changedCount = 0;
        for (UserStorePointsEntity account : accounts) {
            long targetPoints = Math.max(
                    account.getFrozenPoints(), (account.getPoints() + 1L) / 2L);
            long value = targetPoints - account.getPoints();
            if (value == 0) {
                continue;
            }
            changeAccount(account.getUserId(), storeId, value, 0,
                    "STORE_POINTS_HALVE_" + period + "_" + storeId + "_" + account.getUserId(),
                    "SYSTEM", "门店积分定时减半");
            changedCount++;
        }
        log.info("门店积分减半完成 storeId={} period={} accountCount={} changedCount={}",
                storeId, period, accounts.size(), changedCount);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    protected void changeAccount(Long userId, Long storeId, long pointsChange,
                                 long frozenChange, String bizNo,
                                 String operator, String remark) {
        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            UserStorePointsEntity account = getAccount(userId, storeId);
            long targetPoints = Math.addExact(account.getPoints(), pointsChange);
            long targetFrozen = Math.addExact(account.getFrozenPoints(), frozenChange);
            if (targetPoints < 0 || targetFrozen < 0 || targetFrozen > targetPoints) {
                throw new BizException(ErrorCode.POINTS_NOT_ENOUGH);
            }
            if (storePointsMapper.updateAccountOptimistic(
                    userId, storeId, targetPoints, targetFrozen, account.getVersion()) == 1) {
                if (pointsChange != 0) {
                    insertLog(storeId, userId, "POINTS_CHANGE", pointsChange,
                            account.getPoints(), targetPoints, bizNo, operator, remark);
                }
                if (frozenChange != 0) {
                    insertLog(storeId, userId, "FROZEN_CHANGE", frozenChange,
                            account.getFrozenPoints(), targetFrozen,
                            bizNo + "_FROZEN", operator, remark);
                }
                return;
            }
        }
        throw new BizException(ErrorCode.ACCOUNT_CONCURRENT_ERROR);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    protected void changeAccountToPoints(Long userId, Long storeId, long targetPoints,
                                         String bizNo, String operator, String remark) {
        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            UserStorePointsEntity account = getAccount(userId, storeId);
            if (targetPoints < account.getFrozenPoints()) {
                throw new BizException(ErrorCode.POINTS_NOT_ENOUGH);
            }
            long pointsChange = targetPoints - account.getPoints();
            if (pointsChange == 0) {
                return;
            }
            if (storePointsMapper.updateAccountOptimistic(
                    userId, storeId, targetPoints, account.getFrozenPoints(),
                    account.getVersion()) == 1) {
                insertLog(storeId, userId, "POINTS_SET", pointsChange,
                        account.getPoints(), targetPoints, bizNo, operator, remark);
                return;
            }
        }
        throw new BizException(ErrorCode.ACCOUNT_CONCURRENT_ERROR);
    }

    private void insertLog(Long storeId, Long userId, String changeType,
                           long changeValue, long beforeValue, long afterValue,
                           String bizNo, String operator, String remark) {
        StorePointsLogEntity logEntity = new StorePointsLogEntity();
        logEntity.setStoreId(storeId);
        logEntity.setUserId(userId);
        logEntity.setChangeType(changeType);
        logEntity.setChangeValue(changeValue);
        logEntity.setBeforeValue(beforeValue);
        logEntity.setAfterValue(afterValue);
        logEntity.setBizNo(bizNo);
        logEntity.setOperator(operator);
        logEntity.setRemark(remark == null ? "" : remark);
        storePointsMapper.insertLog(logEntity);
    }

    private StorePointsRequestEntity createRequest(
            Long userId, StorePointsApplyCommand command, PointsRequestType type) {
        StorePointsRequestEntity request = new StorePointsRequestEntity();
        request.setStoreId(command.storeId());
        request.setUserId(userId);
        request.setType(type.name());
        request.setPoints(command.points());
        request.setRemark(command.remark());
        request.setStatus("PENDING");
        storePointsMapper.insertRequest(request);
        return request;
    }

    private void applyAuditResult(StorePointsRequestEntity request, boolean approve,
                                  Long adminId, String bizNo) {
        if (PointsRequestType.DEPOSIT.name().equals(request.getType())) {
            if (approve) {
                changeAccount(request.getUserId(), request.getStoreId(),
                        request.getPoints(), 0, bizNo,
                        "ADMIN:" + adminId, "存积分审核通过并计入积分池");
            }
            return;
        }
        if (PointsRequestType.WITHDRAW.name().equals(request.getType())) {
            if (approve) {
                changeAccount(request.getUserId(), request.getStoreId(),
                        -request.getPoints(), -request.getPoints(), bizNo,
                        "ADMIN:" + adminId, "取积分审核通过");
            } else {
                changeAccount(request.getUserId(), request.getStoreId(),
                        0, -request.getPoints(), bizNo,
                        "ADMIN:" + adminId, "取积分审核驳回并解冻");
            }
            return;
        }
        log.error("门店积分申请类型非法 requestId={} type={}",
                request.getId(), request.getType());
        throw new BizException(ErrorCode.INVALID_PARAMETER);
    }

    private StorePointsEntry toEntry(StorePointsRankingRow row) {
        return row == null ? null : new StorePointsEntry(
                row.getRank(), row.getUserId(), row.getNickname(),
                row.getAvatar(), row.getPoints());
    }
}
