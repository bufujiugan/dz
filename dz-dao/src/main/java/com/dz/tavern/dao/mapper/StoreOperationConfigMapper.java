package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.StoreOperationConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StoreOperationConfigMapper {
    StoreOperationConfigEntity selectByStoreId(@Param("storeId") Long storeId);

    StoreOperationConfigEntity selectByStoreIdForUpdate(@Param("storeId") Long storeId);

    List<StoreOperationConfigEntity> selectAll();

    int upsert(StoreOperationConfigEntity config);

    int updateLastPeriod(@Param("storeId") Long storeId,
                         @Param("expectedPeriod") String expectedPeriod,
                         @Param("targetPeriod") String targetPeriod);
}
