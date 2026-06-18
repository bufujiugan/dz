package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {
    int insertOrderItem(OrderItemEntity item);

    List<OrderItemEntity> selectByOrderId(@Param("orderId") Long orderId);
}
