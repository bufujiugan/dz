package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper {
    int insertOperationLog(OperationLogEntity operationLog);

    List<OperationLogEntity> selectPage(@Param("adminId") Long adminId,
                                        @Param("module") String module,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("offset") long offset,
                                        @Param("size") long size);

    long countPage(@Param("adminId") Long adminId,
                   @Param("module") String module,
                   @Param("startTime") LocalDateTime startTime,
                   @Param("endTime") LocalDateTime endTime);
}
