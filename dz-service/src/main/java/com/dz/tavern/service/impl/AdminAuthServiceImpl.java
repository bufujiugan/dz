package com.dz.tavern.service.impl;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.util.JwtUtil;
import com.dz.tavern.dao.entity.AdminUserEntity;
import com.dz.tavern.dao.mapper.AdminUserMapper;
import com.dz.tavern.service.AdminAuthService;
import com.dz.tavern.service.dto.AdminLoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {
    private final AdminUserMapper adminUserMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public AdminLoginResponse login(String username, String password) {
        AdminUserEntity admin = adminUserMapper.selectByUsername(username);
        if (admin == null || admin.getStatus() != 1) {
            log.warn("管理员登录失败 username={} reason=ACCOUNT_UNAVAILABLE", username);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(LocalDateTime.now())) {
            log.warn("管理员登录失败 adminId={} reason=ACCOUNT_LOCKED lockedUntil={}",
                    admin.getId(), admin.getLockedUntil());
            throw new BizException(40102, "账号已临时锁定");
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            int nextFailureCount = admin.getLoginFailCount() + 1;
            LocalDateTime lockedUntil = nextFailureCount >= 5
                    ? LocalDateTime.now().plusMinutes(30) : null;
            adminUserMapper.updateLoginFailure(admin.getId(), lockedUntil);
            log.warn("管理员登录失败 adminId={} failureCount={} locked={}",
                    admin.getId(), nextFailureCount, lockedUntil != null);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        adminUserMapper.updateLoginSuccess(admin.getId(), LocalDateTime.now());
        Set<String> permissions = adminUserMapper.selectPermissions(admin.getRoleId());
        String token = jwtUtil.createToken(admin.getId(), admin.getUsername(), "ADMIN",
                permissions, Duration.ofHours(12));
        log.info("管理员登录成功 adminId={} roleId={} permissionCount={}",
                admin.getId(), admin.getRoleId(), permissions.size());
        return new AdminLoginResponse(token, admin.getRealName(), permissions);
    }
}
