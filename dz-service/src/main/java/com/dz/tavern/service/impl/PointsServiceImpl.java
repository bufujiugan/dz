package com.dz.tavern.service.impl;

import com.dz.tavern.common.annotation.Idempotent;
import com.dz.tavern.common.enums.AccountChangeType;
import com.dz.tavern.common.enums.PointsRequestStatus;
import com.dz.tavern.common.enums.PointsRequestType;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.PointsRequestEntity;
import com.dz.tavern.dao.entity.UserAccountEntity;
import com.dz.tavern.dao.mapper.PointsRequestMapper;
import com.dz.tavern.service.AccountService;
import com.dz.tavern.service.NotificationService;
import com.dz.tavern.service.PointsService;
import com.dz.tavern.service.UserService;
import com.dz.tavern.service.dto.PointsApplyCommand;
import com.dz.tavern.service.dto.PointsAuditCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointsServiceImpl implements PointsService {
    private final PointsRequestMapper pointsRequestMapper;
    private final AccountService accountService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long deposit(Long userId, PointsApplyCommand command) {
        log.warn("用户尝试自助存入积分 userId={}", userId);
        throw new BizException(ErrorCode.POINTS_DEPOSIT_DISABLED);
    }

    @Override
    @Transactional
    public Long withdraw(Long userId, PointsApplyCommand command) {
        userService.checkUserActive(userId);
        Long requestId = createRequest(userId, command, PointsRequestType.WITHDRAW, true);
        log.info("积分提取申请已创建 requestId={} userId={} points={}",
                requestId, userId, command.points());
        return requestId;
    }

    @Override
    public PageResult<PointsRequestEntity> page(Long storeId, Long userId, String type, String status,
                                                long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                pointsRequestMapper.countPage(storeId, userId, type, status),
                pointsRequestMapper.selectPage(storeId, userId, type, status, offset, normalizedSize));
    }

    @Override
    @Transactional
    @Idempotent(key = "#command.requestId")
    public void audit(PointsAuditCommand command, Long adminId) {
        PointsRequestEntity request = pointsRequestMapper.selectById(command.requestId());
        if (request == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!PointsRequestStatus.PENDING.name().equals(request.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        log.info("开始审核积分申请 adminId={} requestId={} userId={} type={} approve={}",
                adminId, request.getId(), request.getUserId(), request.getType(), command.approve());
        String bizNo = "POINTS_REQUEST_" + request.getId();
        UserAccountEntity beforeAccount = accountService.getAccount(request.getUserId());
        if (PointsRequestType.DEPOSIT.name().equals(request.getType()) && command.approve()) {
            accountService.changePoints(request.getUserId(), AccountChangeType.POINTS_ADD,
                    request.getPoints(), bizNo, "ADMIN:" + adminId, "积分存入审核通过");
        } else if (PointsRequestType.WITHDRAW.name().equals(request.getType())) {
            if (command.approve()) {
                accountService.confirmDeductFrozen(request.getUserId(), request.getPoints(),
                        bizNo, "ADMIN:" + adminId);
            } else {
                accountService.unfreezePoints(request.getUserId(), request.getPoints(),
                        bizNo, "ADMIN:" + adminId);
            }
        }
        UserAccountEntity afterAccount = accountService.getAccount(request.getUserId());
        request.setStatus(command.approve()
                ? PointsRequestStatus.APPROVED.name() : PointsRequestStatus.REJECTED.name());
        request.setAuditorId(adminId);
        request.setAuditRemark(command.auditRemark());
        request.setAuditTime(LocalDateTime.now());
        request.setBeforePoints(beforeAccount.getPoints());
        request.setAfterPoints(afterAccount.getPoints());
        if (pointsRequestMapper.updateAudit(request) == 0) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT);
        }
        notificationService.createTask(request.getUserId(), "POINTS_AUDIT",
                "{\"requestId\":" + request.getId() + ",\"status\":\""
                        + request.getStatus() + "\"}");
        log.info("积分申请审核完成 adminId={} requestId={} userId={} status={} beforePoints={} afterPoints={}",
                adminId, request.getId(), request.getUserId(), request.getStatus(),
                request.getBeforePoints(), request.getAfterPoints());
    }

    private Long createRequest(Long userId, PointsApplyCommand command,
                               PointsRequestType type, boolean freeze) {
        PointsRequestEntity request = new PointsRequestEntity();
        request.setType(type.name());
        request.setUserId(userId);
        request.setPoints(command.points());
        request.setRemark(command.remark());
        request.setVoucherImages(toJson(command.voucherImages()));
        request.setStatus(PointsRequestStatus.PENDING.name());
        pointsRequestMapper.insertRequest(request);
        if (freeze) {
            // 提取申请先冻结积分，审核通过后正式扣减，拒绝时再解冻。
            accountService.freezePoints(userId, command.points(),
                    "POINTS_REQUEST_" + request.getId(), "USER:" + userId);
        }
        return request.getId();
    }

    private String toJson(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images == null ? List.of() : images);
        } catch (JsonProcessingException exception) {
            log.warn("积分凭证图片序列化失败 imageCount={}",
                    images == null ? 0 : images.size(), exception);
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
    }
}
