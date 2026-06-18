package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {
    List<CategoryEntity> selectByStoreId(@Param("storeId") Long storeId);

    int insertCategory(CategoryEntity category);

    int updateCategory(CategoryEntity category);

    int deleteCategory(@Param("id") Long id);
}
