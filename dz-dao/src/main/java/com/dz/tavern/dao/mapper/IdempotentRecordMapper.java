package com.dz.tavern.dao.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface IdempotentRecordMapper {
    int tryAcquire(@Param("idempotentKey") String idempotentKey,
                   @Param("expireTime") LocalDateTime expireTime);

    int markSuccess(@Param("idempotentKey") String idempotentKey);

    String selectStatus(@Param("idempotentKey") String idempotentKey);
}
