package com.dz.tavern.service;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.PointsRequestEntity;
import com.dz.tavern.service.dto.PointsApplyCommand;
import com.dz.tavern.service.dto.PointsAuditCommand;

public interface PointsService {
    Long deposit(Long userId, PointsApplyCommand command);

    Long withdraw(Long userId, PointsApplyCommand command);

    default PageResult<PointsRequestEntity> page(Long userId, String type, String status,
                                                 long current, long size) {
        return page(null, userId, type, status, current, size);
    }

    PageResult<PointsRequestEntity> page(Long storeId, Long userId, String type, String status,
                                         long current, long size);

    void audit(PointsAuditCommand command, Long adminId);
}
