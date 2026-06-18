package com.dz.tavern.admin.bootstrap;

import com.dz.tavern.dao.entity.AdminUserEntity;
import com.dz.tavern.dao.mapper.AdminUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final AdminUserMapper adminUserMapper;
    private final String initialPassword;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminBootstrap(AdminUserMapper adminUserMapper,
                          @Value("${security.admin.initial-password:}") String initialPassword) {
        this.adminUserMapper = adminUserMapper;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserMapper.selectByUsername("admin") != null) {
            return;
        }
        // 初始密码必须由部署环境注入，禁止生成后写入日志或固化在代码仓库。
        if (initialPassword == null || initialPassword.isBlank()) {
            throw new IllegalStateException("首次启动必须通过 ADMIN_INITIAL_PASSWORD 配置管理员初始密码");
        }
        AdminUserEntity admin = new AdminUserEntity();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setRealName("超级管理员");
        admin.setRoleId(1L);
        admin.setStatus(1);
        admin.setLoginFailCount(0);
        adminUserMapper.insertAdmin(admin);
        log.warn("首次启动已创建超级管理员 username=admin");
    }
}
