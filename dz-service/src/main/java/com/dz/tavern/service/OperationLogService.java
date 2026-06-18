package com.dz.tavern.service;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.OperationLogEntity;

import java.time.LocalDateTime;

public interface OperationLogService {
    PageResult<OperationLogEntity> page(Long adminId, String module,
                                        LocalDateTime startTime, LocalDateTime endTime,
                                        long current, long size);
}
