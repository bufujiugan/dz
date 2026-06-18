package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.HomeContentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HomeContentMapper {
    HomeContentEntity selectByStoreId(@Param("storeId") Long storeId);

    int upsert(@Param("storeId") Long storeId, @Param("announcement") String announcement);
}
