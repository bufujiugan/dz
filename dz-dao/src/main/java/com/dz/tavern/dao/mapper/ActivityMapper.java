package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.ActivityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityMapper {
    List<ActivityEntity> selectList(@Param("storeId") Long storeId,
                                    @Param("onlyActive") boolean onlyActive);

    ActivityEntity selectById(@Param("id") Long id);

    int insertActivity(ActivityEntity activity);

    int updateActivity(ActivityEntity activity);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
