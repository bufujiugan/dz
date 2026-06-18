package com.dz.tavern.service.impl;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.dao.entity.ActivityEntity;
import com.dz.tavern.dao.entity.SystemConfigEntity;
import com.dz.tavern.dao.entity.StoreOperationConfigEntity;
import com.dz.tavern.dao.mapper.ActivityMapper;
import com.dz.tavern.dao.mapper.SystemConfigMapper;
import com.dz.tavern.dao.mapper.StoreOperationConfigMapper;
import com.dz.tavern.service.ContentService;
import com.dz.tavern.service.dto.PublicContentView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentServiceImpl implements ContentService {
    private static final String GAMEPLAY_KEY = "gameplay.description";
    private static final DateTimeFormatter PERIOD_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> EDITABLE_CONFIG_KEYS = Set.of(
            GAMEPLAY_KEY,
            "points.halving.enabled",
            "points.halving.intervalDays",
            "points.halving.time",
            "coupon.gift.validDays");

    private final ActivityMapper activityMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final StoreOperationConfigMapper storeOperationConfigMapper;

    @Override
    public PublicContentView getPublicContent(Long storeId) {
        StoreOperationConfigEntity config = getStoreConfig(storeId);
        return new PublicContentView(config.getGameplayDescription());
    }

    @Override
    public List<ActivityEntity> listActivities(Long storeId, boolean onlyActive) {
        return activityMapper.selectList(storeId, onlyActive);
    }

    @Override
    public Long saveActivity(ActivityEntity activity) {
        if (activity == null || activity.getStoreId() == null
                || activity.getTitle() == null || activity.getTitle().isBlank()
                || activity.getImageUrl() == null || activity.getImageUrl().isBlank()) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        if (activity.getSort() == null) {
            activity.setSort(0);
        }
        if (activity.getStatus() == null) {
            activity.setStatus(1);
        }
        boolean creating = activity.getId() == null;
        if (creating) {
            activityMapper.insertActivity(activity);
        } else if (activityMapper.updateActivity(activity) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        log.info("活动内容已保存 activityId={} operation={}",
                activity.getId(), creating ? "CREATE" : "UPDATE");
        return activity.getId();
    }

    @Override
    public void changeActivityStatus(Long activityId, Integer status) {
        if (activityId == null || status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        if (activityMapper.updateStatus(activityId, status) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        log.info("活动状态已变更 activityId={} status={}", activityId, status);
    }

    @Override
    public List<SystemConfigEntity> listConfigs() {
        return systemConfigMapper.selectAll();
    }

    @Override
    @Transactional
    public void updateConfigs(Map<String, String> configs) {
        if (configs == null || configs.isEmpty()
                || configs.keySet().stream().anyMatch(key -> !EDITABLE_CONFIG_KEYS.contains(key))) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        configs.forEach((key, value) -> systemConfigMapper.upsert(
                key, value == null ? "" : value.trim(), configDescription(key)));
        log.info("系统参数已更新 configKeys={}", configs.keySet());
    }

    @Override
    public StoreOperationConfigEntity getStoreConfig(Long storeId) {
        if (storeId == null) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        StoreOperationConfigEntity config =
                storeOperationConfigMapper.selectByStoreId(storeId);
        if (config != null) {
            return config;
        }
        StoreOperationConfigEntity defaults = new StoreOperationConfigEntity();
        defaults.setStoreId(storeId);
        defaults.setBusinessEndTime("02:00");
        defaults.setHomeSlogan("今晚，慢一点。");
        defaults.setHeroImage("");
        defaults.setGameplayDescription("积分由酒馆管理员发放，用于本店排行榜荣誉展示。积分按后台配置的自然日周期自动减半，四舍五入。");
        defaults.setMenuTitle("今晚酒单");
        defaults.setPointsHalvingEnabled(0);
        defaults.setPointsHalvingDay(7);
        defaults.setPointsHalvingTime("00:00");
        defaults.setPointsHalvingLastPeriod("");
        return defaults;
    }

    @Override
    public void updateStoreConfig(StoreOperationConfigEntity config) {
        if (config == null || config.getStoreId() == null
                || config.getBusinessEndTime() == null
                || config.getHomeSlogan() == null
                || config.getMenuTitle() == null) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        StoreOperationConfigEntity current = getStoreConfig(config.getStoreId());
        config.setHeroImage(config.getHeroImage() == null ? "" : config.getHeroImage());
        config.setGameplayDescription(config.getGameplayDescription() == null
                ? "" : config.getGameplayDescription());
        config.setPointsHalvingEnabled(config.getPointsHalvingEnabled() == null
                ? 0 : config.getPointsHalvingEnabled());
        config.setPointsHalvingDay(config.getPointsHalvingDay() == null
                ? 7 : config.getPointsHalvingDay());
        if (config.getPointsHalvingDay() < 1) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        config.setPointsHalvingTime(config.getPointsHalvingTime() == null
                ? "00:00" : config.getPointsHalvingTime());
        String lastPeriod = current.getPointsHalvingLastPeriod() == null
                ? "" : current.getPointsHalvingLastPeriod();
        if (config.getPointsHalvingEnabled() == 1 && lastPeriod.isBlank()) {
            lastPeriod = LocalDate.now().format(PERIOD_FORMAT);
        }
        config.setPointsHalvingLastPeriod(lastPeriod);
        storeOperationConfigMapper.upsert(config);
        log.info("门店运营配置已更新 storeId={} businessEndTime={} halvingEnabled={}",
                config.getStoreId(), config.getBusinessEndTime(),
                config.getPointsHalvingEnabled());
    }

    private String configDescription(String key) {
        return switch (key) {
            case GAMEPLAY_KEY -> "小程序玩法说明";
            case "points.halving.enabled" -> "是否启用积分定时减半";
            case "points.halving.intervalDays" -> "积分减半间隔自然日";
            case "points.halving.time" -> "积分减半触发时间";
            case "coupon.gift.validDays" -> "后台赠送卡券有效天数";
            default -> "";
        };
    }
}
