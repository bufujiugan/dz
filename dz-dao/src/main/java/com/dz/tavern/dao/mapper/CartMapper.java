package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.CartEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {
    List<CartEntity> selectByUserId(@Param("userId") Long userId);

    int upsert(@Param("userId") Long userId, @Param("skuId") Long skuId,
               @Param("quantity") Integer quantity);

    int updateQuantity(@Param("id") Long id, @Param("userId") Long userId,
                       @Param("quantity") Integer quantity);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int deleteBySkuIds(@Param("userId") Long userId, @Param("skuIds") List<Long> skuIds);
}
