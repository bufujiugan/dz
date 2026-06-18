package com.dz.tavern.api.schedule;

import com.dz.tavern.service.NotificationService;
import com.dz.tavern.service.OrderService;
import com.dz.tavern.service.CouponService;
import com.dz.tavern.service.PointsHalvingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessSchedule {
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final CouponService couponService;
    private final PointsHalvingService pointsHalvingService;

    @Scheduled(fixedDelay = 60_000)
    public void cancelTimeoutOrders() {
        try {
            orderService.cancelTimeoutOrders();
        } catch (Exception exception) {
            // 本轮失败只记录告警，fixedDelay 调度会在下一周期继续执行。
            log.error("超时订单关闭任务执行失败", exception);
        }
    }

    @Scheduled(fixedDelay = 10_000)
    public void sendNotifications() {
        try {
            notificationService.sendPendingTasks();
        } catch (Exception exception) {
            log.error("订阅通知发送任务执行失败", exception);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void maintainMemberAssets() {
        try {
            couponService.expireCoupons();
            pointsHalvingService.executeIfDue(java.time.LocalDateTime.now());
        } catch (Exception exception) {
            log.error("会员资产定时维护任务执行失败", exception);
        }
    }
}
