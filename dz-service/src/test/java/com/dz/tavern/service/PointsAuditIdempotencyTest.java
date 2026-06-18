package com.dz.tavern.service;

import com.dz.tavern.common.enums.PointsRequestStatus;
import com.dz.tavern.common.enums.PointsRequestType;
import com.dz.tavern.dao.entity.PointsRequestEntity;
import com.dz.tavern.dao.entity.UserAccountEntity;
import com.dz.tavern.dao.mapper.IdempotentRecordMapper;
import com.dz.tavern.dao.mapper.PointsRequestMapper;
import com.dz.tavern.service.aspect.IdempotentAspect;
import com.dz.tavern.service.dto.PointsAuditCommand;
import com.dz.tavern.service.impl.PointsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointsAuditIdempotencyTest {

    @Test
    void duplicateAuditShouldOnlyChangePointsOnce() {
        PointsRequestMapper requestMapper = mock(PointsRequestMapper.class);
        AccountService accountService = mock(AccountService.class);
        NotificationService notificationService = mock(NotificationService.class);
        UserService userService = mock(UserService.class);
        PointsRequestEntity request = new PointsRequestEntity();
        request.setId(1L);
        request.setUserId(1L);
        request.setType(PointsRequestType.DEPOSIT.name());
        request.setStatus(PointsRequestStatus.PENDING.name());
        request.setPoints(50L);
        when(requestMapper.selectById(1L)).thenReturn(request);
        when(requestMapper.updateAudit(any())).thenReturn(1);
        when(accountService.getAccount(1L))
                .thenReturn(account(100L), account(150L));

        PointsServiceImpl target = new PointsServiceImpl(
                requestMapper, accountService, notificationService,
                userService, new ObjectMapper());
        IdempotentRecordMapper idempotentMapper = idempotentMapper();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new IdempotentAspect(idempotentMapper));
        PointsService proxy = factory.getProxy();

        PointsAuditCommand command = new PointsAuditCommand(1L, true, "通过");
        proxy.audit(command, 99L);
        proxy.audit(command, 99L);

        verify(accountService, times(1)).changePoints(
                1L, com.dz.tavern.common.enums.AccountChangeType.POINTS_ADD,
                50L, "POINTS_REQUEST_1", "ADMIN:99", "积分存入审核通过");
        verify(requestMapper, times(1)).updateAudit(any());
    }

    private UserAccountEntity account(long points) {
        UserAccountEntity account = new UserAccountEntity();
        account.setUserId(1L);
        account.setBalanceFen(0L);
        account.setPoints(points);
        account.setFrozenPoints(0L);
        account.setVersion(0);
        return account;
    }

    private IdempotentRecordMapper idempotentMapper() {
        IdempotentRecordMapper mapper = mock(IdempotentRecordMapper.class);
        AtomicBoolean acquired = new AtomicBoolean();
        AtomicBoolean success = new AtomicBoolean();
        when(mapper.tryAcquire(anyString(), any(LocalDateTime.class)))
                .thenAnswer(invocation -> acquired.compareAndSet(false, true) ? 1 : 0);
        when(mapper.markSuccess(anyString())).thenAnswer(invocation -> {
            success.set(true);
            return 1;
        });
        when(mapper.selectStatus(anyString()))
                .thenAnswer(invocation -> success.get() ? "SUCCESS" : "PROCESSING");
        return mapper;
    }
}
