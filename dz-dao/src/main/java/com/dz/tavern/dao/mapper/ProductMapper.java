package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.ProductEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    ProductEntity selectById(@Param("id") Long id, @Param("onlyActive") boolean onlyActive);

    List<ProductEntity> selectByIds(@Param("ids") List<Long> ids,
                                    @Param("onlyActive") boolean onlyActive);

    List<ProductEntity> selectPage(@Param("storeId") Long storeId,
                                   @Param("categoryId") Long categoryId,
                                   @Param("keyword") String keyword,
                                   @Param("onlyActive") boolean onlyActive,
                                   @Param("offset") long offset,
                                   @Param("size") long size);

    long countPage(@Param("storeId") Long storeId,
                   @Param("categoryId") Long categoryId,
                   @Param("keyword") String keyword,
                   @Param("onlyActive") boolean onlyActive);

    List<ProductEntity> selectRecommended(@Param("storeId") Long storeId);

    int insertProduct(ProductEntity product);

    int updateProduct(ProductEntity product);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
