package com.dz.tavern.admin.interceptor;

import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.context.AuthPrincipal;
import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class PermissionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission permission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (permission == null) {
            permission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (permission == null) {
            return true;
        }
        AuthPrincipal principal = LoginContext.get();
        if (!principal.permissions().contains(permission.value())) {
            log.warn("管理员权限校验失败 adminId={} permission={} path={}",
                    principal.id(), permission.value(), request.getRequestURI());
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
