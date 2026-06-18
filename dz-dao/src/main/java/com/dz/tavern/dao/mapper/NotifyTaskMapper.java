package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.NotifyTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotifyTaskMapper {
    int insertTask(NotifyTaskEntity task);

    List<NotifyTaskEntity> selectPending(@Param("now") LocalDateTime now,
                                         @Param("limit") int limit);

    int markSuccess(@Param("id") Long id);

    int markFailure(@Param("id") Long id, @Param("retryCount") Integer retryCount,
                    @Param("status") String status, @Param("nextRetryTime") LocalDateTime nextRetryTime,
                    @Param("lastError") String lastError);
}
