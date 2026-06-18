package com.dz.tavern.service.impl;

import com.dz.tavern.common.annotation.Idempotent;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.CouponCategoryEntity;
import com.dz.tavern.dao.entity.CouponRedeemRequestEntity;
import com.dz.tavern.dao.entity.CouponTemplateEntity;
import com.dz.tavern.dao.entity.OrderItemEntity;
import com.dz.tavern.dao.entity.ProductSkuEntity;
import com.dz.tavern.dao.entity.SystemConfigEntity;
import com.dz.tavern.dao.entity.UserCouponEntity;
import com.dz.tavern.dao.mapper.CouponMapper;
import com.dz.tavern.dao.mapper.ProductSkuMapper;
import com.dz.tavern.dao.mapper.SystemConfigMapper;
import com.dz.tavern.dao.mapper.UserMapper;
import com.dz.tavern.service.CouponService;
import com.dz.tavern.service.NotificationService;
import com.dz.tavern.service.dto.CouponAuditCommand;
import com.dz.tavern.service.dto.CouponGiftCommand;
import com.dz.tavern.service.dto.CouponRedeemCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {
    private static final String UNUSED = "UNUSED";
    private static final String REDEEM_PENDING = "REDEEM_PENDING";

    private final CouponMapper couponMapper;
    private final ProductSkuMapper productSkuMapper;
    private final UserMapper userMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final NotificationService notificationService;

    @Override
    public List<CouponCategoryEntity> listCategories(Long storeId, boolean onlyActive) {
        return couponMapper.selectCategories(storeId, onlyActive);
    }

    @Override
    public List<CouponTemplateEntity> listTemplates(Long storeId, boolean onlyActive) {
        return couponMapper.selectTemplates(storeId, onlyActive);
    }

    @Override
    public Long saveTemplate(CouponTemplateEntity template) {
        validateTemplate(template);
        boolean creating = template.getId() == null;
        if (creating) {
            couponMapper.insertTemplate(template);
        } else if (couponMapper.updateTemplate(template) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        log.info("卡券模板已保存 templateId={} saleProductId={} operation={}",
                template.getId(), template.getSaleProductId(),
                creating ? "CREATE" : "UPDATE");
        return template.getId();
    }

    @Override
    public void changeTemplateStatus(Long templateId, Integer status) {
        if (templateId == null || status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        if (couponMapper.updateTemplateStatus(templateId, status) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        log.info("卡券模板状态已变更 templateId={} status={}", templateId, status);
    }

    @Override
    @Transactional
    public void issuePurchasedCoupons(Long userId, String orderNo, List<OrderItemEntity> items) {
        int issuedCount = 0;
        for (OrderItemEntity item : items) {
            ProductSkuEntity sku = productSkuMapper.selectById(item.getSkuId());
            if (sku == null) {
                continue;
            }
            CouponTemplateEntity template =
                    couponMapper.selectTemplateByProductId(sku.getProductId());
            if (template == null) {
                continue;
            }
            for (int index = 1; index <= item.getQuantity(); index++) {
                String sourceNo = orderNo + ":" + item.getId() + ":" + index;
                issuedCount += issueCoupon(userId, template, "PURCHASE", sourceNo,
                        template.getPurchaseValidDays());
            }
        }
        if (issuedCount > 0) {
            log.info("购买卡券已发放 userId={} orderNo={} issuedCount={}",
                    userId, orderNo, issuedCount);
        }
    }

    @Override
    @Transactional
    public void gift(CouponGiftCommand command, Long adminId) {
        CouponTemplateEntity template = requireTemplate(command.templateId());
        int quantity = command.quantity() == null ? 1 : command.quantity();
        int validDays = giftValidDays();
        Set<Long> userIds = new LinkedHashSet<>(command.userIds());
        String giftBatch = "GIFT:" + adminId + ":" + System.currentTimeMillis();
        int issuedCount = 0;
        for (Long userId : userIds) {
            if (userMapper.selectById(userId) == null) {
                throw new BizException(ErrorCode.NOT_FOUND);
            }
            for (int index = 1; index <= quantity; index++) {
                issuedCount += issueCoupon(userId, template, "GIFT",
                        giftBatch + ":" + userId + ":" + index, validDays);
            }
        }
        log.info("管理员赠送卡券完成 adminId={} templateId={} userCount={} issuedCount={}",
                adminId, template.getId(), userIds.size(), issuedCount);
    }

    @Override
    public List<UserCouponEntity> listUserCoupons(Long userId, Long storeId, String status) {
        expireCoupons();
        return couponMapper.selectUserCoupons(userId, storeId, status);
    }

    @Override
    @Transactional
    public Long requestRedeem(Long userId, CouponRedeemCommand command) {
        LocalDateTime now = LocalDateTime.now();
        if (couponMapper.markRedeemPending(command.userCouponId(), userId, now) == 0) {
            throw new BizException(ErrorCode.COUPON_STATE_INVALID);
        }
        CouponRedeemRequestEntity request = new CouponRedeemRequestEntity();
        request.setUserCouponId(command.userCouponId());
        request.setUserId(userId);
        request.setStatus("PENDING");
        request.setRemark(command.remark() == null ? "" : command.remark());
        couponMapper.insertRedeemRequest(request);
        log.info("卡券核销申请已创建 requestId={} userId={} userCouponId={}",
                request.getId(), userId, command.userCouponId());
        return request.getId();
    }

    @Override
    public PageResult<UserCouponEntity> adminPageCoupons(
            Long storeId, Long userId, String status, long current, long size) {
        expireCoupons();
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                couponMapper.countAdminUserCoupons(storeId, userId, status),
                couponMapper.selectAdminUserCoupons(storeId, userId, status, offset, normalizedSize));
    }

    @Override
    public PageResult<CouponRedeemRequestEntity> pageRedeemRequests(
            Long storeId, String status, long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                couponMapper.countRedeemRequests(storeId, status),
                couponMapper.selectRedeemRequests(storeId, status, offset, normalizedSize));
    }

    @Override
    @Transactional
    @Idempotent(key = "#command.requestId")
    public void auditRedeem(CouponAuditCommand command, Long adminId) {
        CouponRedeemRequestEntity request =
                couponMapper.selectRedeemRequestById(command.requestId());
        if (request == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BizException(ErrorCode.COUPON_STATE_INVALID);
        }
        UserCouponEntity coupon = couponMapper.selectUserCouponById(request.getUserCouponId());
        if (coupon == null || !REDEEM_PENDING.equals(coupon.getStatus())) {
            throw new BizException(ErrorCode.COUPON_STATE_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        String targetCouponStatus = command.approve()
                ? "USED" : (coupon.getExpireTime().isAfter(now) ? UNUSED : "EXPIRED");
        if (couponMapper.updateUserCouponStatus(
                coupon.getId(), REDEEM_PENDING, targetCouponStatus,
                command.approve() ? now : null) == 0) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT);
        }
        request.setStatus(command.approve() ? "APPROVED" : "REJECTED");
        request.setAuditorId(adminId);
        request.setAuditRemark(command.auditRemark());
        request.setAuditTime(now);
        if (couponMapper.updateRedeemAudit(request) == 0) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT);
        }
        notificationService.createTask(request.getUserId(), "COUPON_REDEEM_AUDIT",
                "{\"requestId\":" + request.getId() + ",\"status\":\""
                        + request.getStatus() + "\"}");
        log.info("卡券核销审核完成 adminId={} requestId={} userId={} approve={}",
                adminId, request.getId(), request.getUserId(), command.approve());
    }

    @Override
    public void expireCoupons() {
        int count = couponMapper.expireUnused(LocalDateTime.now());
        if (count > 0) {
            log.info("过期卡券状态更新完成 count={}", count);
        }
    }

    private int issueCoupon(Long userId, CouponTemplateEntity template,
                            String sourceType, String sourceNo, int validDays) {
        UserCouponEntity coupon = new UserCouponEntity();
        coupon.setCouponNo("CP" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        coupon.setTemplateId(template.getId());
        coupon.setUserId(userId);
        coupon.setStoreId(template.getStoreId());
        coupon.setCouponName(template.getName());
        coupon.setImageUrl(template.getImageUrl());
        coupon.setSourceType(sourceType);
        coupon.setSourceNo(sourceNo);
        coupon.setStatus(UNUSED);
        coupon.setExpireTime(LocalDateTime.now().plusDays(validDays));
        return couponMapper.insertUserCoupon(coupon);
    }

    private CouponTemplateEntity requireTemplate(Long templateId) {
        CouponTemplateEntity template = couponMapper.selectTemplateById(templateId);
        if (template == null || template.getStatus() == null || template.getStatus() != 1) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return template;
    }

    private void validateTemplate(CouponTemplateEntity template) {
        if (template == null || template.getStoreId() == null
                || template.getName() == null || template.getName().isBlank()) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        if (template.getImageUrl() == null) {
            template.setImageUrl("");
        }
        if (template.getDescription() == null) {
            template.setDescription("");
        }
        if (template.getPurchaseValidDays() == null || template.getPurchaseValidDays() <= 0) {
            template.setPurchaseValidDays(365);
        }
        if (template.getGiftValidDays() == null || template.getGiftValidDays() <= 0) {
            template.setGiftValidDays(30);
        }
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
    }

    private int giftValidDays() {
        SystemConfigEntity config = systemConfigMapper.selectByKey("coupon.gift.validDays");
        try {
            return config == null ? 30 : Math.max(1, Integer.parseInt(config.getConfigValue()));
        } catch (NumberFormatException exception) {
            log.warn("赠送卡券有效天数配置无效 configValue={}",
                    config == null ? null : config.getConfigValue(), exception);
            return 30;
        }
    }
}
