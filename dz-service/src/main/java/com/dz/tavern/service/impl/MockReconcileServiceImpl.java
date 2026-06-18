package com.dz.tavern.service.impl;

import com.dz.tavern.service.ReconcileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "wechat.mock-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockReconcileServiceImpl implements ReconcileService {
    @Override
    public Map<String, Object> reconcileDaily(LocalDate date) {
        log.info("执行每日对账 date={} mode=MOCK", date);
        return Map.of(
                "date", date,
                "mode", "MOCK",
                "differences", List.of(),
                "message", "mock 模式未连接微信账单，数据库比对流程已保留扩展接口");
    }
}
