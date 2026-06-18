package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    int insertOrder(OrderEntity order);

    OrderEntity selectByOrderNo(@Param("orderNo") String orderNo);

    int updateStatus(@Param("orderNo") String orderNo,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("targetStatus") String targetStatus,
                     @Param("eventTime") LocalDateTime eventTime);

    int markPrepayRequested(@Param("orderNo") String orderNo);

    List<OrderEntity> selectUserPage(@Param("userId") Long userId,
                                     @Param("storeId") Long storeId,
                                     @Param("status") String status,
                                     @Param("offset") long offset,
                                     @Param("size") long size);

    long countUserPage(@Param("userId") Long userId,
                       @Param("storeId") Long storeId,
                       @Param("status") String status);

    List<OrderEntity> selectTimeoutCreated(@Param("deadline") LocalDateTime deadline,
                                           @Param("limit") int limit);

    List<OrderEntity> selectAdminPage(@Param("storeId") Long storeId,
                                      @Param("orderNo") String orderNo,
                                      @Param("status") String status,
                                      @Param("offset") long offset,
                                      @Param("size") long size);

    long countAdminPage(@Param("storeId") Long storeId,
                        @Param("orderNo") String orderNo,
                        @Param("status") String status);
}
