package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.RechargeTierEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RechargeTierMapper {
    RechargeTierEntity selectById(@Param("id") Long id);

    List<RechargeTierEntity> selectActiveList();

    int insertTier(RechargeTierEntity tier);

    int updateTier(RechargeTierEntity tier);

    int deleteTier(@Param("id") Long id);
}
