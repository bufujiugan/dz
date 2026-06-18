package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.ProductSkuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductSkuMapper {
    ProductSkuEntity selectById(@Param("id") Long id);

    List<ProductSkuEntity> selectByIds(@Param("ids") List<Long> ids);

    List<ProductSkuEntity> selectByProductIds(@Param("productIds") List<Long> productIds);

    List<ProductSkuEntity> selectByProductId(@Param("productId") Long productId);

    int decreaseStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    int increaseStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    int insertSku(ProductSkuEntity sku);

    int updateSku(ProductSkuEntity sku);
}
