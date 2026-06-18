package com.dz.tavern.service;

import com.dz.tavern.dao.entity.StorePointsLogEntity;
import com.dz.tavern.dao.entity.StorePointsRequestEntity;
import com.dz.tavern.dao.entity.UserStorePointsEntity;
import com.dz.tavern.dao.mapper.StorePointsMapper;
import com.dz.tavern.service.dto.StorePointsApplyCommand;
import com.dz.tavern.service.dto.StorePointsAuditCommand;
import com.dz.tavern.service.dto.StorePointsSetCommand;
import com.dz.tavern.service.impl.StorePointsServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorePointsServiceImplTest {

    @Test
    void depositShouldWaitForAuditBeforeAddingPoints() {
        StorePointsMapper mapper = mock(StorePointsMapper.class);
        UserService userService = mock(UserService.class);
        NotificationService notificationService = mock(NotificationService.class);
        when(mapper.selectAccount(10001L, 10001L)).thenReturn(account(1680L));
        when(mapper.insertRequest(any())).thenAnswer(invocation -> {
            StorePointsRequestEntity request = invocation.getArgument(0);
            request.setId(90001L);
            return 1;
        });
        StorePointsService service = new StorePointsServiceImpl(
                mapper, userService, notificationService);

        service.deposit(10001L,
                new StorePointsApplyCommand(10001L, 100L, "线下消费积分存入"));

        verify(mapper).insertRequest(any(StorePointsRequestEntity.class));
        verify(mapper, never()).updateAccountOptimistic(
                any(), any(), any(), any(), any());
    }

    @Test
    void approvedDepositShouldAddPointsToStorePool() {
        StorePointsMapper mapper = mock(StorePointsMapper.class);
        UserService userService = mock(UserService.class);
        NotificationService notificationService = mock(NotificationService.class);
        StorePointsRequestEntity request = new StorePointsRequestEntity();
        request.setId(90001L);
        request.setStoreId(10001L);
        request.setUserId(10001L);
        request.setType("DEPOSIT");
        request.setPoints(100L);
        request.setStatus("PENDING");
        when(mapper.selectRequestById(90001L)).thenReturn(request);
        when(mapper.selectAccount(10001L, 10001L))
                .thenReturn(account(1680L), account(1680L), account(1780L));
        when(mapper.updateAccountOptimistic(
                10001L, 10001L, 1780L, 0L, 0)).thenReturn(1);
        when(mapper.updateRequestAudit(any())).thenReturn(1);
        StorePointsService service = new StorePointsServiceImpl(
                mapper, userService, notificationService);

        service.audit(new StorePointsAuditCommand(90001L, true, "凭证有效"), 1L);

        verify(mapper).updateAccountOptimistic(
                10001L, 10001L, 1780L, 0L, 0);
        verify(mapper).insertLog(any());
        verify(mapper).updateRequestAudit(request);
    }

    @Test
    void setPointsShouldWriteTargetPointsAndLogDifference() {
        StorePointsMapper mapper = mock(StorePointsMapper.class);
        UserService userService = mock(UserService.class);
        NotificationService notificationService = mock(NotificationService.class);
        when(mapper.selectAccount(10001L, 10001L)).thenReturn(account(1680L));
        when(mapper.updateAccountOptimistic(
                10001L, 10001L, 24444L, 0L, 0)).thenReturn(1);
        StorePointsService service = new StorePointsServiceImpl(
                mapper, userService, notificationService);

        service.setPoints(
                new StorePointsSetCommand(10001L, 10001L, 24444L, "后台校准积分"),
                1L);

        verify(mapper).updateAccountOptimistic(
                10001L, 10001L, 24444L, 0L, 0);
        ArgumentCaptor<StorePointsLogEntity> logCaptor =
                ArgumentCaptor.forClass(StorePointsLogEntity.class);
        verify(mapper).insertLog(logCaptor.capture());
        StorePointsLogEntity log = logCaptor.getValue();
        assertThat(log.getChangeType()).isEqualTo("POINTS_SET");
        assertThat(log.getChangeValue()).isEqualTo(22764L);
        assertThat(log.getBeforeValue()).isEqualTo(1680L);
        assertThat(log.getAfterValue()).isEqualTo(24444L);
    }

    private UserStorePointsEntity account(long points) {
        UserStorePointsEntity account = new UserStorePointsEntity();
        account.setUserId(10001L);
        account.setStoreId(10001L);
        account.setPoints(points);
        account.setFrozenPoints(0L);
        account.setVersion(0);
        return account;
    }
}
