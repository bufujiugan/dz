package com.dz.tavern.service;

import com.dz.tavern.common.enums.PayType;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.dao.entity.OrderEntity;
import com.dz.tavern.dao.entity.ProductEntity;
import com.dz.tavern.dao.entity.ProductSkuEntity;
import com.dz.tavern.dao.mapper.CartMapper;
import com.dz.tavern.dao.mapper.OrderItemMapper;
import com.dz.tavern.dao.mapper.OrderMapper;
import com.dz.tavern.dao.mapper.ProductMapper;
import com.dz.tavern.dao.mapper.ProductSkuMapper;
import com.dz.tavern.service.dto.OrderCreateCommand;
import com.dz.tavern.service.dto.OrderItemCommand;
import com.dz.tavern.service.impl.OrderServiceImpl;
import com.dz.tavern.service.payment.WechatPayGateway;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderOversellTest {

    @Test
    void stock10Concurrent20ShouldSucceedExactly10() throws Exception {
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        CartMapper cartMapper = mock(CartMapper.class);
        AccountService accountService = mock(AccountService.class);
        UserService userService = mock(UserService.class);
        WechatPayGateway gateway = mock(WechatPayGateway.class);
        CouponService couponService = mock(CouponService.class);

        ProductSkuEntity sku = new ProductSkuEntity();
        sku.setId(1L);
        sku.setProductId(1L);
        sku.setSpecName("500ml");
        sku.setPriceFen(100L);
        ProductEntity product = new ProductEntity();
        product.setId(1L);
        product.setStoreId(1L);
        product.setName("测试商品");

        when(skuMapper.selectById(1L)).thenReturn(sku);
        when(productMapper.selectById(1L, true)).thenReturn(product);
        AtomicInteger stock = new AtomicInteger(10);
        when(skuMapper.decreaseStock(anyLong(), anyInt())).thenAnswer(invocation -> {
            while (true) {
                int current = stock.get();
                if (current <= 0) {
                    return 0;
                }
                if (stock.compareAndSet(current, current - 1)) {
                    return 1;
                }
            }
        });
        AtomicLong orderId = new AtomicLong();
        when(orderMapper.insertOrder(any(OrderEntity.class))).thenAnswer(invocation -> {
            invocation.<OrderEntity>getArgument(0).setId(orderId.incrementAndGet());
            return 1;
        });
        when(orderItemMapper.insertOrderItem(any())).thenReturn(1);
        when(cartMapper.deleteBySkuIds(anyLong(), any())).thenReturn(1);
        doNothing().when(userService).checkUserActive(1L);

        OrderService service = new OrderServiceImpl(
                orderMapper, orderItemMapper, productMapper, skuMapper, cartMapper,
                accountService, userService, new OrderStateMachine(), gateway,
                couponService);
        OrderCreateCommand command = new OrderCreateCommand(
                1L, List.of(new OrderItemCommand(1L, 1, 100L)),
                PayType.WECHAT, "");

        int requestCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int index = 0; index < requestCount; index++) {
            futures.add(executor.submit(() -> {
                start.await();
                try {
                    service.create(1L, command);
                    return true;
                } catch (BizException exception) {
                    return false;
                }
            }));
        }
        start.countDown();
        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }
        executor.shutdownNow();

        assertThat(successCount).isEqualTo(10);
        assertThat(stock.get()).isZero();
    }
}
