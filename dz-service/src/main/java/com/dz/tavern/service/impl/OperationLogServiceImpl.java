package com.dz.tavern.service.impl;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.OperationLogEntity;
import com.dz.tavern.dao.mapper.OperationLogMapper;
import com.dz.tavern.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {
    private final OperationLogMapper operationLogMapper;

    @Override
    public PageResult<OperationLogEntity> page(Long adminId, String module,
                                               LocalDateTime startTime, LocalDateTime endTime,
                                               long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                operationLogMapper.countPage(adminId, module, startTime, endTime),
                operationLogMapper.selectPage(
                        adminId, module, startTime, endTime, offset, normalizedSize));
    }
}
