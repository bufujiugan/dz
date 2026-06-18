package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.SystemConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemConfigMapper {
    SystemConfigEntity selectByKey(@Param("configKey") String configKey);

    SystemConfigEntity selectByKeyForUpdate(@Param("configKey") String configKey);

    List<SystemConfigEntity> selectAll();

    int upsert(@Param("configKey") String configKey,
               @Param("configValue") String configValue,
               @Param("description") String description);
}
