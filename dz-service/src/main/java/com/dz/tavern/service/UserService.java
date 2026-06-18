package com.dz.tavern.service;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.UserEntity;
import com.dz.tavern.service.dto.ProfileUpdateRequest;
import com.dz.tavern.service.dto.LeaderboardEntry;
import com.dz.tavern.service.dto.UserProfile;

import java.util.List;

public interface UserService {
    UserProfile getProfile(Long userId);

    void updateProfile(Long userId, ProfileUpdateRequest request);

    void checkUserActive(Long userId);

    PageResult<UserEntity> pageUsers(Long storeId, String keyword, long current, long size);

    default PageResult<UserEntity> pageUsers(String keyword, long current, long size) {
        return pageUsers(null, keyword, current, size);
    }

    void changeStatus(Long userId, Integer status);

    List<LeaderboardEntry> pointsLeaderboard(int limit);
}
