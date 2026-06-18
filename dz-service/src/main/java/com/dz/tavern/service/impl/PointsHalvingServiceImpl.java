package com.dz.tavern.service.impl;

import com.dz.tavern.dao.entity.StoreOperationConfigEntity;
import com.dz.tavern.dao.mapper.StoreOperationConfigMapper;
import com.dz.tavern.service.PointsHalvingService;
import com.dz.tavern.service.StorePointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointsHalvingServiceImpl implements PointsHalvingService {
    private static final DateTimeFormatter PERIOD_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StoreOperationConfigMapper storeOperationConfigMapper;
    private final StorePointsService storePointsService;

    @Override
    public void executeIfDue(LocalDateTime now) {
        for (StoreOperationConfigEntity config : storeOperationConfigMapper.selectAll()) {
            executeStoreIfDue(config, now);
        }
    }

    private void executeStoreIfDue(StoreOperationConfigEntity config, LocalDateTime now) {
        if (config.getPointsHalvingEnabled() == null
                || config.getPointsHalvingEnabled() != 1) {
            return;
        }
        int intervalDays = parseIntervalDays(config.getPointsHalvingDay() == null
                ? "7" : String.valueOf(config.getPointsHalvingDay()));
        LocalTime triggerTime = parseTime(config.getPointsHalvingTime());
        String lastPeriod = config.getPointsHalvingLastPeriod() == null
                ? "" : config.getPointsHalvingLastPeriod();
        if (lastPeriod.isBlank()) {
            initializeHalvingPeriod(config, now);
            return;
        }
        LocalDate lastDate = parseLastPeriod(lastPeriod, config.getStoreId());
        if (lastDate == null) {
            return;
        }
        LocalDate dueDate = lastDate.plusDays(intervalDays);
        LocalDateTime dueTime = dueDate.atTime(triggerTime);
        if (dueTime.isAfter(now)) {
            return;
        }
        while (!dueDate.plusDays(intervalDays).atTime(triggerTime).isAfter(now)) {
            dueDate = dueDate.plusDays(intervalDays);
        }
        dueTime = dueDate.atTime(triggerTime);
        String period = dueDate.format(PERIOD_FORMAT);
        storePointsService.halveStorePoints(config.getStoreId(), period);
        if (storeOperationConfigMapper.updateLastPeriod(
                config.getStoreId(), lastPeriod, period) == 0) {
            log.warn("门店积分减半周期标记更新冲突 storeId={} period={}",
                    config.getStoreId(), period);
            return;
        }
        log.info("门店积分定时减半执行完成 storeId={} period={} dueTime={}",
                config.getStoreId(), period, dueTime);
    }

    private void initializeHalvingPeriod(StoreOperationConfigEntity config, LocalDateTime now) {
        String period = now.toLocalDate().format(PERIOD_FORMAT);
        if (storeOperationConfigMapper.updateLastPeriod(config.getStoreId(), "", period) == 0) {
            log.warn("门店积分减半初始周期标记更新冲突 storeId={} period={}",
                    config.getStoreId(), period);
            return;
        }
        log.info("门店积分减半初始周期已记录 storeId={} period={}",
                config.getStoreId(), period);
    }

    private int parseIntervalDays(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            log.warn("积分减半间隔天数配置无效 configValue={}", value, exception);
            return 7;
        }
    }

    private LocalDate parseLastPeriod(String value, Long storeId) {
        try {
            return LocalDate.parse(value, PERIOD_FORMAT);
        } catch (DateTimeParseException exception) {
            log.warn("积分减半最后执行周期无效 storeId={} lastPeriod={}",
                    storeId, value, exception);
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (RuntimeException exception) {
            log.warn("积分减半时间配置无效 configValue={}", value, exception);
            return LocalTime.MIDNIGHT;
        }
    }
}
