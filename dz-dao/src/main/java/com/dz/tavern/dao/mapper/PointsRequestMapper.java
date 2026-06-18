package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.PointsRequestEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PointsRequestMapper {
    int insertRequest(PointsRequestEntity request);

    PointsRequestEntity selectById(@Param("id") Long id);

    int updateAudit(PointsRequestEntity request);

    List<PointsRequestEntity> selectPage(@Param("storeId") Long storeId,
                                         @Param("userId") Long userId,
                                         @Param("type") String type,
                                         @Param("status") String status,
                                         @Param("offset") long offset,
                                         @Param("size") long size);

    long countPage(@Param("storeId") Long storeId,
                   @Param("userId") Long userId,
                   @Param("type") String type,
                   @Param("status") String status);
}
