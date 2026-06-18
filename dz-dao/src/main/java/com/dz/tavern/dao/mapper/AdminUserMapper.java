package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.AdminUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Set;

@Mapper
public interface AdminUserMapper {
    AdminUserEntity selectByUsername(@Param("username") String username);

    Set<String> selectPermissions(@Param("roleId") Long roleId);

    int updateLoginSuccess(@Param("id") Long id, @Param("loginTime") LocalDateTime loginTime);

    int updateLoginFailure(@Param("id") Long id, @Param("lockedUntil") LocalDateTime lockedUntil);

    int insertAdmin(AdminUserEntity adminUser);
}
