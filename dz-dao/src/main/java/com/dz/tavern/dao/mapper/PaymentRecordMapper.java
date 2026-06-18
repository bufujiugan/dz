package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.PaymentRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentRecordMapper {
    PaymentRecordEntity selectByOrderNo(@Param("orderNo") String orderNo);

    int insertRecord(PaymentRecordEntity record);

    int increaseNotifyCount(@Param("orderNo") String orderNo,
                            @Param("notifyRaw") String notifyRaw,
                            @Param("verifyResult") String verifyResult);

    List<PaymentRecordEntity> selectPage(@Param("storeId") Long storeId,
                                         @Param("orderNo") String orderNo,
                                         @Param("tradeState") String tradeState,
                                         @Param("offset") long offset,
                                         @Param("size") long size);

    long countPage(@Param("storeId") Long storeId,
                   @Param("orderNo") String orderNo,
                   @Param("tradeState") String tradeState);
}
