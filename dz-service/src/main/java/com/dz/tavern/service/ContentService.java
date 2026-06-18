package com.dz.tavern.service;

import com.dz.tavern.dao.entity.ActivityEntity;
import com.dz.tavern.dao.entity.SystemConfigEntity;
import com.dz.tavern.dao.entity.StoreOperationConfigEntity;
import com.dz.tavern.service.dto.PublicContentView;

import java.util.List;
import java.util.Map;

public interface ContentService {
    PublicContentView getPublicContent(Long storeId);

    List<ActivityEntity> listActivities(Long storeId, boolean onlyActive);

    Long saveActivity(ActivityEntity activity);

    void changeActivityStatus(Long activityId, Integer status);

    List<SystemConfigEntity> listConfigs();

    void updateConfigs(Map<String, String> configs);

    StoreOperationConfigEntity getStoreConfig(Long storeId);

    void updateStoreConfig(StoreOperationConfigEntity config);
}
