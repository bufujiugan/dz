package com.dz.tavern.service.impl;

import com.dz.tavern.common.annotation.Idempotent;
import com.dz.tavern.common.enums.AccountChangeType;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.common.util.BizNoGenerator;
import com.dz.tavern.dao.entity.RechargeOrderEntity;
import com.dz.tavern.dao.entity.RechargeTierEntity;
import com.dz.tavern.dao.mapper.RechargeOrderMapper;
import com.dz.tavern.dao.mapper.RechargeTierMapper;
import com.dz.tavern.service.AccountService;
import com.dz.tavern.service.NotificationService;
import com.dz.tavern.service.RechargeService;
import com.dz.tavern.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RechargeServiceImpl implements RechargeService {
    private final RechargeTierMapper rechargeTierMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final AccountService accountService;
    private final NotificationService notificationService;
    private final UserService userService;

    @Override
    public List<RechargeTierEntity> listTiers() {
        return rechargeTierMapper.selectActiveList();
    }

    @Override
    public RechargeOrderEntity create(Long userId, Long tierId) {
        log.info("开始创建充值单 userId={} tierId={}", userId, tierId);
        userService.checkUserActive(userId);
        RechargeTierEntity tier = rechargeTierMapper.selectById(tierId);
        if (tier == null || tier.getStatus() != 1) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        RechargeOrderEntity order = new RechargeOrderEntity();
        order.setRechargeNo(BizNoGenerator.rechargeNo());
        order.setUserId(userId);
        order.setTierId(tierId);
        order.setPayFen(tier.getPayFen());
        order.setBonusFen(tier.getBonusFen());
        order.setStatus("CREATED");
        order.setCredited(0);
        order.setPrepayRequested(0);
        rechargeOrderMapper.insertOrder(order);
        log.info("充值单创建完成 userId={} rechargeNo={} tierId={} payFen={} bonusFen={}",
                userId, order.getRechargeNo(), tierId, order.getPayFen(), order.getBonusFen());
        return order;
    }

    @Override
    @Transactional
    @Idempotent(key = "#rechargeNo")
    public void credit(String rechargeNo, String operator) {
        RechargeOrderEntity order = requireOrder(rechargeNo);
        log.info("开始充值入账 rechargeNo={} userId={} status={} credited={}",
                rechargeNo, order.getUserId(), order.getStatus(), order.getCredited());
        if ("CREATED".equals(order.getStatus())) {
            rechargeOrderMapper.markPaid(rechargeNo);
            order.setStatus("PAID");
            log.info("充值单状态已变更 rechargeNo={} userId={} toStatus=PAID",
                    rechargeNo, order.getUserId());
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        // 先通过条件更新抢占入账权，重复回调不会再次增加用户余额。
        if (order.getCredited() == 1 || rechargeOrderMapper.markCredited(rechargeNo) == 0) {
            log.info("充值单已入账，忽略重复处理 rechargeNo={} userId={}",
                    rechargeNo, order.getUserId());
            return;
        }
        accountService.changeBalance(order.getUserId(), AccountChangeType.RECHARGE,
                order.getPayFen(), rechargeNo, operator, "充值本金");
        if (order.getBonusFen() > 0) {
            accountService.changeBalance(order.getUserId(), AccountChangeType.RECHARGE,
                    order.getBonusFen(), rechargeNo, operator, "充值赠送");
        }
        notificationService.createTask(order.getUserId(), "RECHARGE_SUCCESS",
                "{\"rechargeNo\":\"" + rechargeNo + "\"}");
        log.info("充值入账完成 rechargeNo={} userId={} payFen={} bonusFen={}",
                rechargeNo, order.getUserId(), order.getPayFen(), order.getBonusFen());
    }

    @Override
    public PageResult<RechargeOrderEntity> page(Long userId, long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                rechargeOrderMapper.countUserPage(userId),
                rechargeOrderMapper.selectUserPage(userId, offset, normalizedSize));
    }

    @Override
    @Transactional
    @Idempotent(key = "#rechargeNo")
    public void manualCredit(String rechargeNo, Long adminId) {
        RechargeOrderEntity order = requireOrder(rechargeNo);
        log.info("管理员发起手工充值入账 adminId={} rechargeNo={} userId={}",
                adminId, rechargeNo, order.getUserId());
        if (!"PAID".equals(order.getStatus()) || order.getCredited() == 1) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        credit(rechargeNo, "ADMIN:" + adminId);
    }

    @Override
    public PageResult<RechargeOrderEntity> adminPage(Long storeId, String rechargeNo, String status,
                                                     long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                rechargeOrderMapper.countAdminPage(storeId, rechargeNo, status),
                rechargeOrderMapper.selectAdminPage(storeId, rechargeNo, status, offset, normalizedSize));
    }

    @Override
    public Long saveTier(RechargeTierEntity tier) {
        boolean creating = tier.getId() == null;
        if (tier.getId() == null) {
            rechargeTierMapper.insertTier(tier);
        } else {
            rechargeTierMapper.updateTier(tier);
        }
        log.info("充值档位已保存 tierId={} operation={} payFen={} bonusFen={} status={}",
                tier.getId(), creating ? "CREATE" : "UPDATE",
                tier.getPayFen(), tier.getBonusFen(), tier.getStatus());
        return tier.getId();
    }

    @Override
    public void deleteTier(Long tierId) {
        rechargeTierMapper.deleteTier(tierId);
        log.info("充值档位已删除 tierId={}", tierId);
    }

    private RechargeOrderEntity requireOrder(String rechargeNo) {
        RechargeOrderEntity order = rechargeOrderMapper.selectByRechargeNo(rechargeNo);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return order;
    }
}
