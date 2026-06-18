package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.CouponRedeemRequestEntity;
import com.dz.tavern.dao.entity.CouponCategoryEntity;
import com.dz.tavern.dao.entity.CouponTemplateEntity;
import com.dz.tavern.dao.entity.UserCouponEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CouponMapper {
    List<CouponCategoryEntity> selectCategories(@Param("storeId") Long storeId,
                                                @Param("onlyActive") boolean onlyActive);

    List<CouponTemplateEntity> selectTemplates(@Param("storeId") Long storeId,
                                               @Param("onlyActive") boolean onlyActive);

    CouponTemplateEntity selectTemplateById(@Param("id") Long id);

    CouponTemplateEntity selectTemplateByProductId(@Param("productId") Long productId);

    int insertTemplate(CouponTemplateEntity template);

    int updateTemplate(CouponTemplateEntity template);

    int updateTemplateStatus(@Param("id") Long id, @Param("status") Integer status);

    int insertUserCoupon(UserCouponEntity coupon);

    UserCouponEntity selectUserCouponById(@Param("id") Long id);

    List<UserCouponEntity> selectUserCoupons(@Param("userId") Long userId,
                                             @Param("storeId") Long storeId,
                                             @Param("status") String status);

    List<UserCouponEntity> selectAdminUserCoupons(@Param("storeId") Long storeId,
                                                  @Param("userId") Long userId,
                                                  @Param("status") String status,
                                                  @Param("offset") long offset,
                                                  @Param("size") long size);

    long countAdminUserCoupons(@Param("storeId") Long storeId,
                               @Param("userId") Long userId,
                               @Param("status") String status);

    int markRedeemPending(@Param("id") Long id, @Param("userId") Long userId,
                          @Param("now") LocalDateTime now);

    int updateUserCouponStatus(@Param("id") Long id,
                               @Param("expectedStatus") String expectedStatus,
                               @Param("targetStatus") String targetStatus,
                               @Param("usedTime") LocalDateTime usedTime);

    int expireUnused(@Param("now") LocalDateTime now);

    int insertRedeemRequest(CouponRedeemRequestEntity request);

    CouponRedeemRequestEntity selectRedeemRequestById(@Param("id") Long id);

    List<CouponRedeemRequestEntity> selectRedeemRequests(
            @Param("storeId") Long storeId, @Param("status") String status,
            @Param("offset") long offset,
            @Param("size") long size);

    long countRedeemRequests(@Param("storeId") Long storeId,
                             @Param("status") String status);

    int updateRedeemAudit(CouponRedeemRequestEntity request);
}
