package com.dz.tavern.service;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.CouponCategoryEntity;
import com.dz.tavern.dao.entity.CouponRedeemRequestEntity;
import com.dz.tavern.dao.entity.CouponTemplateEntity;
import com.dz.tavern.dao.entity.OrderItemEntity;
import com.dz.tavern.dao.entity.UserCouponEntity;
import com.dz.tavern.service.dto.CouponAuditCommand;
import com.dz.tavern.service.dto.CouponGiftCommand;
import com.dz.tavern.service.dto.CouponRedeemCommand;

import java.util.List;

public interface CouponService {
    List<CouponCategoryEntity> listCategories(Long storeId, boolean onlyActive);

    List<CouponTemplateEntity> listTemplates(Long storeId, boolean onlyActive);

    Long saveTemplate(CouponTemplateEntity template);

    void changeTemplateStatus(Long templateId, Integer status);

    void issuePurchasedCoupons(Long userId, String orderNo, List<OrderItemEntity> items);

    void gift(CouponGiftCommand command, Long adminId);

    List<UserCouponEntity> listUserCoupons(Long userId, Long storeId, String status);

    Long requestRedeem(Long userId, CouponRedeemCommand command);

    PageResult<UserCouponEntity> adminPageCoupons(
            Long storeId, Long userId, String status, long current, long size);

    PageResult<CouponRedeemRequestEntity> pageRedeemRequests(
            Long storeId, String status, long current, long size);

    void auditRedeem(CouponAuditCommand command, Long adminId);

    void expireCoupons();
}
