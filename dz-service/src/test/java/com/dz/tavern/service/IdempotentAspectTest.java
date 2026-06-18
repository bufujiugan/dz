package com.dz.tavern.service;

import com.dz.tavern.common.annotation.Idempotent;
import com.dz.tavern.dao.mapper.IdempotentRecordMapper;
import com.dz.tavern.service.aspect.IdempotentAspect;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdempotentAspectTest {

    @Test
    void concurrentRequestsShouldOnlyExecuteOnce() throws Exception {
        AtomicBoolean acquired = new AtomicBoolean();
        AtomicBoolean success = new AtomicBoolean();
        IdempotentRecordMapper mapper = mock(IdempotentRecordMapper.class);
        when(mapper.tryAcquire(anyString(), any(LocalDateTime.class)))
                .thenAnswer(invocation -> acquired.compareAndSet(false, true) ? 1 : 0);
        when(mapper.selectStatus(anyString()))
                .thenAnswer(invocation -> success.get() ? "SUCCESS" : "PROCESSING");
        when(mapper.markSuccess(anyString())).thenAnswer(invocation -> {
            success.set(true);
            return 1;
        });

        AtomicInteger executions = new AtomicInteger();
        DemoService target = new DemoServiceImpl(executions);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new IdempotentAspect(mapper));
        DemoService proxy = factory.getProxy();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int index = 0; index < threadCount; index++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    proxy.execute("same-key");
                } catch (RuntimeException ignored) {
                    // PROCESSING 状态下的并发请求应被拒绝。
                }
                return null;
            }));
        }
        ready.await();
        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdownNow();

        assertThat(executions.get()).isEqualTo(1);
    }

    interface DemoService {
        void execute(String key) throws InterruptedException;
    }

    static class DemoServiceImpl implements DemoService {
        private final AtomicInteger executions;

        DemoServiceImpl(AtomicInteger executions) {
            this.executions = executions;
        }

        @Override
        @Idempotent(key = "#key")
        public void execute(String key) throws InterruptedException {
            executions.incrementAndGet();
            Thread.sleep(80);
        }
    }
}
