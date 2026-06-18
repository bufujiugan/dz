package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.StoreEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StoreMapper {
    List<StoreEntity> selectActiveList();

    List<StoreEntity> selectPage(@Param("keyword") String keyword,
                                 @Param("status") Integer status,
                                 @Param("offset") long offset,
                                 @Param("size") long size);

    long countPage(@Param("keyword") String keyword,
                   @Param("status") Integer status);

    StoreEntity selectById(@Param("id") Long id);

    int insertStore(StoreEntity store);

    int updateStore(StoreEntity store);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
