package com.dz.tavern.service.impl;

import com.dz.tavern.common.enums.AccountChangeType;
import com.dz.tavern.common.enums.OrderStatus;
import com.dz.tavern.common.enums.PayType;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.common.util.BizNoGenerator;
import com.dz.tavern.dao.entity.OrderEntity;
import com.dz.tavern.dao.entity.OrderItemEntity;
import com.dz.tavern.dao.entity.ProductEntity;
import com.dz.tavern.dao.entity.ProductSkuEntity;
import com.dz.tavern.dao.mapper.CartMapper;
import com.dz.tavern.dao.mapper.OrderItemMapper;
import com.dz.tavern.dao.mapper.OrderMapper;
import com.dz.tavern.dao.mapper.ProductMapper;
import com.dz.tavern.dao.mapper.ProductSkuMapper;
import com.dz.tavern.service.AccountService;
import com.dz.tavern.service.CouponService;
import com.dz.tavern.service.OrderService;
import com.dz.tavern.service.OrderStateMachine;
import com.dz.tavern.service.UserService;
import com.dz.tavern.service.dto.OrderCreateCommand;
import com.dz.tavern.service.dto.OrderCreateResult;
import com.dz.tavern.service.dto.OrderDetail;
import com.dz.tavern.service.dto.OrderItemCommand;
import com.dz.tavern.service.payment.WechatPayGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CartMapper cartMapper;
    private final AccountService accountService;
    private final UserService userService;
    private final OrderStateMachine orderStateMachine;
    private final WechatPayGateway wechatPayGateway;
    private final CouponService couponService;

    @Override
    @Transactional
    public OrderCreateResult create(Long userId, OrderCreateCommand command) {
        log.info("开始创建订单 userId={} storeId={} payType={} itemCount={}",
                userId, command.storeId(), command.payType(), command.items().size());
        userService.checkUserActive(userId);
        if (command.payType() == PayType.POINTS) {
            throw new BizException(ErrorCode.POINTS_PAYMENT_DISABLED);
        }
        List<ItemSnapshot> snapshots = new ArrayList<>();
        long totalFen = 0L;
        for (OrderItemCommand item : command.items()) {
            ProductSkuEntity sku = productSkuMapper.selectById(item.skuId());
            if (sku == null) {
                throw new BizException(ErrorCode.NOT_FOUND);
            }
            ProductEntity product = productMapper.selectById(sku.getProductId(), true);
            if (product == null || !product.getStoreId().equals(command.storeId())) {
                throw new BizException(ErrorCode.NOT_FOUND);
            }
            if (item.expectedPriceFen() != null
                    && !item.expectedPriceFen().equals(sku.getPriceFen())) {
                throw new BizException(ErrorCode.PRICE_CHANGED);
            }
            // 库存通过条件更新原子扣减；任一商品失败时，当前事务会回滚已扣减的库存。
            if (productSkuMapper.decreaseStock(sku.getId(), item.quantity()) == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
            }
            long itemTotal = Math.multiplyExact(sku.getPriceFen(), item.quantity().longValue());
            totalFen = Math.addExact(totalFen, itemTotal);
            snapshots.add(new ItemSnapshot(product, sku, item.quantity()));
        }

        OrderEntity order = new OrderEntity();
        order.setOrderNo(BizNoGenerator.orderNo());
        order.setUserId(userId);
        order.setStoreId(command.storeId());
        order.setTotalFen(totalFen);
        order.setPayType(command.payType().name());
        order.setStatus(OrderStatus.CREATED.name());
        order.setRemark(command.remark() == null ? "" : command.remark());
        order.setPrepayRequested(0);
        orderMapper.insertOrder(order);

        for (ItemSnapshot snapshot : snapshots) {
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(order.getId());
            orderItem.setSkuId(snapshot.sku().getId());
            orderItem.setProductName(snapshot.product().getName());
            orderItem.setSpecName(snapshot.sku().getSpecName());
            orderItem.setPriceFen(snapshot.sku().getPriceFen());
            orderItem.setQuantity(snapshot.quantity());
            orderItemMapper.insertOrderItem(orderItem);
        }
        cartMapper.deleteBySkuIds(userId,
                snapshots.stream().map(snapshot -> snapshot.sku().getId()).toList());

        if (command.payType() == PayType.BALANCE) {
            accountService.deductBalance(userId, totalFen, order.getOrderNo(), "USER:" + userId);
            transition(order, OrderStatus.PAID);
            couponService.issuePurchasedCoupons(userId, order.getOrderNo(),
                    orderItemMapper.selectByOrderId(order.getId()));
        }
        log.info("订单创建完成 userId={} orderNo={} totalFen={} status={}",
                userId, order.getOrderNo(), totalFen, order.getStatus());
        return new OrderCreateResult(order.getOrderNo(), totalFen,
                OrderStatus.valueOf(order.getStatus()));
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long storeId, String orderNo) {
        log.info("用户申请取消订单 userId={} storeId={} orderNo={}", userId, storeId, orderNo);
        OrderEntity order = requireOrder(orderNo);
        validateUserStore(order, userId, storeId);
        cancelCreatedOrder(order);
    }

    @Override
    public PageResult<OrderEntity> page(Long userId, Long storeId, String status,
                                        long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                orderMapper.countUserPage(userId, storeId, status),
                orderMapper.selectUserPage(userId, storeId, status, offset, normalizedSize));
    }

    @Override
    public OrderDetail detail(Long userId, Long storeId, String orderNo) {
        OrderEntity order = requireOrder(orderNo);
        validateUserStore(order, userId, storeId);
        return new OrderDetail(order, orderItemMapper.selectByOrderId(order.getId()));
    }

    @Override
    @Transactional
    public void cancelTimeoutOrders() {
        // 每批最多处理 100 条，避免一次定时任务持有过多锁并形成长事务。
        List<OrderEntity> timeoutOrders = orderMapper.selectTimeoutCreated(
                LocalDateTime.now().minusMinutes(15), 100);
        timeoutOrders.forEach(this::cancelCreatedOrder);
        if (!timeoutOrders.isEmpty()) {
            log.info("超时订单关闭完成 count={}", timeoutOrders.size());
        }
    }

    @Override
    public PageResult<OrderEntity> adminPage(Long storeId, String orderNo, String status,
                                             long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                orderMapper.countAdminPage(storeId, orderNo, status),
                orderMapper.selectAdminPage(storeId, orderNo, status, offset, normalizedSize));
    }

    @Override
    @Transactional
    public void complete(String orderNo) {
        transition(requireOrder(orderNo), OrderStatus.COMPLETED);
    }

    @Override
    @Transactional
    public void markPaid(String orderNo) {
        OrderEntity order = requireOrder(orderNo);
        if (OrderStatus.PAID.name().equals(order.getStatus())) {
            return;
        }
        transition(order, OrderStatus.PAID);
        couponService.issuePurchasedCoupons(order.getUserId(), order.getOrderNo(),
                orderItemMapper.selectByOrderId(order.getId()));
    }

    @Override
    @Transactional
    public void refundBalanceOrder(String orderNo, String operator) {
        OrderEntity order = requireOrder(orderNo);
        log.info("开始处理站内资产退款 orderNo={} userId={} payType={} amountFen={}",
                orderNo, order.getUserId(), order.getPayType(), order.getTotalFen());
        startRefund(orderNo);
        if (PayType.POINTS.name().equals(order.getPayType())) {
            accountService.changePoints(order.getUserId(), AccountChangeType.REFUND,
                    order.getTotalFen(), orderNo, operator, "积分支付退款");
        } else {
            accountService.changeBalance(order.getUserId(), AccountChangeType.REFUND,
                    order.getTotalFen(), orderNo, operator, "余额支付退款");
        }
        completeRefund(orderNo);
        log.info("站内资产退款完成 orderNo={} userId={}", orderNo, order.getUserId());
    }

    @Override
    @Transactional
    public void startRefund(String orderNo) {
        transition(requireOrder(orderNo), OrderStatus.REFUNDING);
    }

    @Override
    @Transactional
    public void completeRefund(String orderNo) {
        transition(requireOrder(orderNo), OrderStatus.REFUNDED);
    }

    private void cancelCreatedOrder(OrderEntity order) {
        transition(order, OrderStatus.CANCELLED);
        for (OrderItemEntity item : orderItemMapper.selectByOrderId(order.getId())) {
            productSkuMapper.increaseStock(item.getSkuId(), item.getQuantity());
        }
        if (order.getPrepayRequested() != null && order.getPrepayRequested() == 1) {
            log.info("调用微信关闭预支付订单 orderNo={} userId={}",
                    order.getOrderNo(), order.getUserId());
            wechatPayGateway.close(order.getOrderNo());
        }
    }

    private void transition(OrderEntity order, OrderStatus target) {
        OrderStatus current = OrderStatus.valueOf(order.getStatus());
        orderStateMachine.validate(current, target);
        // 数据库更新同时校验旧状态，防止并发请求重复推进订单状态。
        if (orderMapper.updateStatus(order.getOrderNo(), current.name(), target.name(),
                LocalDateTime.now()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        order.setStatus(target.name());
        log.info("订单状态已变更 orderNo={} userId={} fromStatus={} toStatus={}",
                order.getOrderNo(), order.getUserId(), current, target);
    }

    private OrderEntity requireOrder(String orderNo) {
        OrderEntity order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return order;
    }

    private void validateUserStore(OrderEntity order, Long userId, Long storeId) {
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (storeId != null && !order.getStoreId().equals(storeId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
    }

    private record ItemSnapshot(ProductEntity product, ProductSkuEntity sku, Integer quantity) {
    }
}
