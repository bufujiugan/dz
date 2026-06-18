package com.dz.tavern.service;

public interface NotificationService {
    void createTask(Long userId, String templateType, String jsonData);

    void sendPendingTasks();
}
