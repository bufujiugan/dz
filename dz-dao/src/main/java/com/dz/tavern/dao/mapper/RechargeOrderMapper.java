package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.RechargeOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RechargeOrderMapper {
    int insertOrder(RechargeOrderEntity order);

    RechargeOrderEntity selectByRechargeNo(@Param("rechargeNo") String rechargeNo);

    int markPaid(@Param("rechargeNo") String rechargeNo);

    int markCredited(@Param("rechargeNo") String rechargeNo);

    int markPrepayRequested(@Param("rechargeNo") String rechargeNo);

    List<RechargeOrderEntity> selectUserPage(@Param("userId") Long userId,
                                             @Param("offset") long offset,
                                             @Param("size") long size);

    long countUserPage(@Param("userId") Long userId);

    List<RechargeOrderEntity> selectAdminPage(@Param("storeId") Long storeId,
                                              @Param("rechargeNo") String rechargeNo,
                                              @Param("status") String status,
                                              @Param("offset") long offset,
                                              @Param("size") long size);

    long countAdminPage(@Param("storeId") Long storeId,
                        @Param("rechargeNo") String rechargeNo,
                        @Param("status") String status);
}
