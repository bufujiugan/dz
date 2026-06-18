package com.dz.tavern.service.impl;

import com.dz.tavern.common.enums.AccountChangeType;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.AccountLogEntity;
import com.dz.tavern.dao.entity.UserAccountEntity;
import com.dz.tavern.dao.mapper.UserAccountMapper;
import com.dz.tavern.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private static final int MAX_RETRY_TIMES = 3;
    private final UserAccountMapper userAccountMapper;

    @Override
    public UserAccountEntity getAccount(Long userId) {
        UserAccountEntity account = userAccountMapper.selectByUserId(userId);
        if (account == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return account;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void changeBalance(Long userId, AccountChangeType changeType, long changeValue,
                              String bizNo, String operator, String remark) {
        // 账户采用版本号乐观锁；短暂冲突在事务内重试，避免并发覆盖其他资产变更。
        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            UserAccountEntity account = getAccount(userId);
            long targetBalance = Math.addExact(account.getBalanceFen(), changeValue);
            if (targetBalance < 0) {
                throw new BizException(ErrorCode.BALANCE_NOT_ENOUGH);
            }
            if (updateAccount(account, targetBalance, account.getPoints(), account.getFrozenPoints())) {
                insertLog(userId, "BALANCE", changeType, changeValue,
                        account.getBalanceFen(), targetBalance, bizNo, operator, remark);
                return;
            }
        }
        log.warn("余额变更并发重试耗尽 userId={} bizNo={} changeType={}",
                userId, bizNo, changeType);
        throw new BizException(ErrorCode.ACCOUNT_CONCURRENT_ERROR);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void changePoints(Long userId, AccountChangeType changeType, long changeValue,
                             String bizNo, String operator, String remark) {
        // 可用积分不能低于冻结积分，避免提现审核期间发生超额消费。
        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            UserAccountEntity account = getAccount(userId);
            long targetPoints = Math.addExact(account.getPoints(), changeValue);
            if (targetPoints < account.getFrozenPoints()) {
                throw new BizException(ErrorCode.POINTS_NOT_ENOUGH);
            }
            if (updateAccount(account, account.getBalanceFen(), targetPoints, account.getFrozenPoints())) {
                insertLog(userId, "POINTS", changeType, changeValue,
                        account.getPoints(), targetPoints, bizNo, operator, remark);
                return;
            }
        }
        log.warn("积分变更并发重试耗尽 userId={} bizNo={} changeType={}",
                userId, bizNo, changeType);
        throw new BizException(ErrorCode.ACCOUNT_CONCURRENT_ERROR);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deductBalance(Long userId, long amountFen, String orderNo, String operator) {
        if (amountFen <= 0) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        changeBalance(userId, AccountChangeType.PAY_BALANCE, -amountFen,
                orderNo, operator, "余额支付");
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void freezePoints(Long userId, long points, String bizNo, String operator) {
        requirePositive(points);
        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            UserAccountEntity account = getAccount(userId);
            long targetFrozen = Math.addExact(account.getFrozenPoints(), points);
            if (targetFrozen > account.getPoints()) {
                throw new BizException(ErrorCode.POINTS_NOT_ENOUGH);
            }
            if (updateAccount(account, account.getBalanceFen(), account.getPoints(), targetFrozen)) {
                insertLog(userId, "FROZEN_POINTS", AccountChangeType.POINTS_FREEZE, points,
                        account.getFrozenPoints(), targetFrozen, bizNo, operator, "冻结积分");
                return;
            }
        }
        throw new BizException(ErrorCode.ACCOUNT_CONCURRENT_ERROR);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void confirmDeductFrozen(Long userId, long points, String bizNo, String operator) {
        requirePositive(points);
        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            UserAccountEntity account = getAccount(userId);
            long targetPoints = account.getPoints() - points;
            long targetFrozen = account.getFrozenPoints() - points;
            if (targetPoints < 0 || targetFrozen < 0) {
                throw new BizException(ErrorCode.POINTS_NOT_ENOUGH);
            }
            if (updateAccount(account, account.getBalanceFen(), targetPoints, targetFrozen)) {
                insertLog(userId, "POINTS", AccountChangeType.POINTS_DEDUCT, -points,
                        account.getPoints(), targetPoints, bizNo, operator, "确认扣减冻结积分");
                insertLog(userId, "FROZEN_POINTS", AccountChangeType.POINTS_DEDUCT, -points,
                        account.getFrozenPoints(), targetFrozen, bizNo, operator, "释放已扣减冻结积分");
                return;
            }
        }
        throw new BizException(ErrorCode.ACCOUNT_CONCURRENT_ERROR);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void unfreezePoints(Long userId, long points, String bizNo, String operator) {
        requirePositive(points);
        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            UserAccountEntity account = getAccount(userId);
            long targetFrozen = account.getFrozenPoints() - points;
            if (targetFrozen < 0) {
                throw new BizException(ErrorCode.POINTS_NOT_ENOUGH);
            }
            if (updateAccount(account, account.getBalanceFen(), account.getPoints(), targetFrozen)) {
                insertLog(userId, "FROZEN_POINTS", AccountChangeType.POINTS_UNFREEZE, -points,
                        account.getFrozenPoints(), targetFrozen, bizNo, operator, "解冻积分");
                return;
            }
        }
        throw new BizException(ErrorCode.ACCOUNT_CONCURRENT_ERROR);
    }

    @Override
    public PageResult<AccountLogEntity> pageLogs(Long storeId, Long userId, String changeType,
                                                 YearMonth month, String bizNo,
                                                 long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        LocalDateTime startTime = month == null ? null : month.atDay(1).atStartOfDay();
        LocalDateTime endTime = month == null ? null : month.plusMonths(1).atDay(1).atStartOfDay();
        long offset = (normalizedCurrent - 1) * normalizedSize;
        List<AccountLogEntity> records = userAccountMapper.selectLogs(
                storeId, userId, changeType, startTime, endTime, bizNo, offset, normalizedSize);
        long total = userAccountMapper.countLogs(storeId, userId, changeType, startTime, endTime, bizNo);
        return new PageResult<>(normalizedCurrent, normalizedSize, total, records);
    }

    private boolean updateAccount(UserAccountEntity account, long balanceFen,
                                  long points, long frozenPoints) {
        return userAccountMapper.updateAccountOptimistic(
                account.getUserId(), balanceFen, points, frozenPoints, account.getVersion()) == 1;
    }

    private void insertLog(Long userId, String assetType, AccountChangeType changeType,
                           long changeValue, long beforeValue, long afterValue,
                           String bizNo, String operator, String remark) {
        AccountLogEntity accountLog = new AccountLogEntity();
        accountLog.setUserId(userId);
        accountLog.setAssetType(assetType);
        accountLog.setChangeType(changeType.name());
        accountLog.setChangeValue(changeValue);
        accountLog.setBeforeValue(beforeValue);
        accountLog.setAfterValue(afterValue);
        accountLog.setBizNo(bizNo);
        accountLog.setOperator(operator);
        accountLog.setRemark(remark == null ? "" : remark);
        userAccountMapper.insertAccountLog(accountLog);
        log.info("用户资产已变更 userId={} bizNo={} assetType={} changeType={} changeValue={} beforeValue={} afterValue={}",
                userId, bizNo, assetType, changeType, changeValue, beforeValue, afterValue);
    }

    private void requirePositive(long value) {
        if (value <= 0) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
    }
}
