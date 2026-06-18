package com.dz.tavern.service;

import com.dz.tavern.dao.entity.StoreOperationConfigEntity;
import com.dz.tavern.dao.mapper.StoreOperationConfigMapper;
import com.dz.tavern.service.impl.PointsHalvingServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointsHalvingServiceImplTest {

    @Test
    void executeIfDueShouldUseConfigurableIntervalDays() {
        StoreOperationConfigMapper mapper = mock(StoreOperationConfigMapper.class);
        StorePointsService storePointsService = mock(StorePointsService.class);
        StoreOperationConfigEntity config = config(10001L, 1, 3, "00:00", "2026-06-10");
        when(mapper.selectAll()).thenReturn(List.of(config));
        when(mapper.updateLastPeriod(10001L, "2026-06-10", "2026-06-16"))
                .thenReturn(1);
        PointsHalvingService service = new PointsHalvingServiceImpl(
                mapper, storePointsService);

        service.executeIfDue(LocalDateTime.of(2026, 6, 16, 1, 0));

        verify(storePointsService).halveStorePoints(10001L, "2026-06-16");
        verify(mapper).updateLastPeriod(10001L, "2026-06-10", "2026-06-16");
    }

    @Test
    void executeIfDueShouldInitializeBlankPeriodWithoutHalvingImmediately() {
        StoreOperationConfigMapper mapper = mock(StoreOperationConfigMapper.class);
        StorePointsService storePointsService = mock(StorePointsService.class);
        StoreOperationConfigEntity config = config(10001L, 1, 7, "00:00", "");
        when(mapper.selectAll()).thenReturn(List.of(config));
        when(mapper.updateLastPeriod(10001L, "", "2026-06-16")).thenReturn(1);
        PointsHalvingService service = new PointsHalvingServiceImpl(
                mapper, storePointsService);

        service.executeIfDue(LocalDateTime.of(2026, 6, 16, 1, 0));

        verify(storePointsService, never()).halveStorePoints(10001L, "2026-06-16");
        verify(mapper).updateLastPeriod(10001L, "", "2026-06-16");
    }

    private StoreOperationConfigEntity config(Long storeId, Integer enabled,
                                              Integer intervalDays, String triggerTime,
                                              String lastPeriod) {
        StoreOperationConfigEntity config = new StoreOperationConfigEntity();
        config.setStoreId(storeId);
        config.setPointsHalvingEnabled(enabled);
        config.setPointsHalvingDay(intervalDays);
        config.setPointsHalvingTime(triggerTime);
        config.setPointsHalvingLastPeriod(lastPeriod);
        return config;
    }
}
