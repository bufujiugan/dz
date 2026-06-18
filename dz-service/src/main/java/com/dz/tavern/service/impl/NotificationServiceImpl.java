package com.dz.tavern.service.impl;

import com.dz.tavern.dao.entity.NotifyTaskEntity;
import com.dz.tavern.dao.entity.UserEntity;
import com.dz.tavern.dao.mapper.NotifyTaskMapper;
import com.dz.tavern.dao.mapper.UserMapper;
import com.dz.tavern.service.NotificationService;
import com.dz.tavern.service.wechat.WechatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotifyTaskMapper notifyTaskMapper;
    private final UserMapper userMapper;
    private final WechatClient wechatClient;

    @Override
    public void createTask(Long userId, String templateType, String jsonData) {
        NotifyTaskEntity task = new NotifyTaskEntity();
        task.setUserId(userId);
        task.setTemplateType(templateType);
        task.setData(jsonData);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setNextRetryTime(LocalDateTime.now());
        notifyTaskMapper.insertTask(task);
        log.info("订阅通知任务已创建 taskId={} userId={} templateType={}",
                task.getId(), userId, templateType);
    }

    @Override
    public void sendPendingTasks() {
        List<NotifyTaskEntity> tasks = notifyTaskMapper.selectPending(LocalDateTime.now(), 50);
        if (!tasks.isEmpty()) {
            log.info("开始发送待处理订阅通知 count={}", tasks.size());
        }
        for (NotifyTaskEntity task : tasks) {
            try {
                UserEntity user = userMapper.selectById(task.getUserId());
                if (user == null) {
                    throw new IllegalStateException("用户不存在");
                }
                wechatClient.sendSubscribeMessage(
                        user.getOpenid(), task.getTemplateType(), task.getData());
                notifyTaskMapper.markSuccess(task.getId());
                log.info("订阅通知发送成功 taskId={} userId={} templateType={}",
                        task.getId(), task.getUserId(), task.getTemplateType());
            } catch (Exception exception) {
                int retryCount = task.getRetryCount() + 1;
                String status = retryCount >= 3 ? "FAILED" : "PENDING";
                // 失败任务按次数递增退避，达到上限后转为 FAILED，等待人工检查。
                notifyTaskMapper.markFailure(task.getId(), retryCount, status,
                        LocalDateTime.now().plusSeconds(10L * retryCount),
                        abbreviate(exception.getMessage()));
                log.error("订阅通知发送失败 taskId={} userId={} templateType={} retryCount={} status={}",
                        task.getId(), task.getUserId(), task.getTemplateType(),
                        retryCount, status, exception);
            }
        }
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "unknown";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
