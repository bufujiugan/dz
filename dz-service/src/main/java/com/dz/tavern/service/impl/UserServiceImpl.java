package com.dz.tavern.service.impl;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.common.util.SensitiveMask;
import com.dz.tavern.dao.entity.UserAccountEntity;
import com.dz.tavern.dao.entity.UserEntity;
import com.dz.tavern.dao.mapper.UserAccountMapper;
import com.dz.tavern.dao.mapper.UserMapper;
import com.dz.tavern.service.UserService;
import com.dz.tavern.service.dto.ProfileUpdateRequest;
import com.dz.tavern.service.dto.LeaderboardEntry;
import com.dz.tavern.service.dto.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final UserAccountMapper userAccountMapper;

    @Override
    public UserProfile getProfile(Long userId) {
        UserEntity user = requireUser(userId);
        UserAccountEntity account = userAccountMapper.selectByUserId(userId);
        if (account == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return new UserProfile(user.getId(), user.getNickname(), user.getAvatar(),
                SensitiveMask.phone(user.getPhone()), account.getBalanceFen(), account.getPoints(),
                account.getFrozenPoints(), user.getStatus());
    }

    @Override
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        requireUser(userId);
        userMapper.updateProfile(userId, request.nickname(), request.avatar());
        log.info("用户资料已更新 userId={}", userId);
    }

    @Override
    public void checkUserActive(Long userId) {
        if (requireUser(userId).getStatus() == 1) {
            throw new BizException(ErrorCode.USER_BANNED);
        }
    }

    @Override
    public PageResult<UserEntity> pageUsers(Long storeId, String keyword, long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        List<UserEntity> users = userMapper.selectPage(storeId, keyword, offset, normalizedSize);
        users.forEach(user -> {
            user.setPhone(SensitiveMask.phone(user.getPhone()));
            user.setOpenid(SensitiveMask.openid(user.getOpenid()));
        });
        return new PageResult<>(normalizedCurrent, normalizedSize,
                userMapper.countPage(storeId, keyword), users);
    }

    @Override
    public void changeStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        requireUser(userId);
        userMapper.updateStatus(userId, status);
        log.info("用户状态已变更 userId={} status={}", userId, status);
    }

    @Override
    public List<LeaderboardEntry> pointsLeaderboard(int limit) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 20);
        List<com.dz.tavern.dao.projection.PointsRankingRow> rows =
                userAccountMapper.selectPointsRanking(normalizedLimit);
        return java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(index -> {
                    com.dz.tavern.dao.projection.PointsRankingRow row = rows.get(index);
                    return new LeaderboardEntry(index + 1, row.getUserId(),
                            row.getNickname(), row.getAvatar(), row.getPoints());
                })
                .toList();
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return user;
    }
}
