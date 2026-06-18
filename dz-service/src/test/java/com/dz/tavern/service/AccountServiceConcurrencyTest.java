package com.dz.tavern.service;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.dao.entity.UserAccountEntity;
import com.dz.tavern.dao.mapper.UserAccountMapper;
import com.dz.tavern.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountServiceConcurrencyTest {

    @Test
    void balance100ConcurrentDeduct60ShouldOnlySucceedOnce() throws Exception {
        UserAccountMapper mapper = mock(UserAccountMapper.class);
        AtomicLong balance = new AtomicLong(100);
        AtomicInteger version = new AtomicInteger();
        AtomicInteger logCount = new AtomicInteger();

        when(mapper.selectByUserId(1L)).thenAnswer(invocation ->
                account(balance.get(), version.get()));
        when(mapper.updateAccountOptimistic(anyLong(), anyLong(), anyLong(), anyLong(), anyInt()))
                .thenAnswer(invocation -> {
                    long targetBalance = invocation.getArgument(1);
                    int expectedVersion = invocation.getArgument(4);
                    synchronized (balance) {
                        if (version.get() != expectedVersion) {
                            return 0;
                        }
                        balance.set(targetBalance);
                        version.incrementAndGet();
                        return 1;
                    }
                });
        when(mapper.insertAccountLog(any())).thenAnswer(invocation -> {
            logCount.incrementAndGet();
            return 1;
        });

        AccountService service = new AccountServiceImpl(mapper);
        Callable<Boolean> task = () -> {
            try {
                service.deductBalance(1L, 60, "ORDER-1", "TEST");
                return true;
            } catch (BizException exception) {
                return false;
            }
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> results = executor.invokeAll(List.of(task, task)).stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                }).toList();
        executor.shutdownNow();

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(balance.get()).isEqualTo(40);
        assertThat(logCount.get()).isEqualTo(1);
    }

    private UserAccountEntity account(long balance, int version) {
        UserAccountEntity account = new UserAccountEntity();
        account.setUserId(1L);
        account.setBalanceFen(balance);
        account.setPoints(0L);
        account.setFrozenPoints(0L);
        account.setVersion(version);
        return account;
    }
}
