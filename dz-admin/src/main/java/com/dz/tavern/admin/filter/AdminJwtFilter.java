package com.dz.tavern.admin.filter;

import com.dz.tavern.common.context.AuthPrincipal;
import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminJwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/")
                || path.equals("/admin")
                || path.equals("/favicon.ico")
                || path.equals("/error")
                || path.startsWith("/admin/")
                || path.equals("/admin-api/auth/login")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return;
        }
        AuthPrincipal principal;
        try {
            principal = jwtUtil.parseToken(authorization.substring(7));
        } catch (Exception exception) {
            log.warn("管理员令牌解析失败 path={} remoteAddr={}",
                    request.getRequestURI(), request.getRemoteAddr(), exception);
            writeUnauthorized(response);
            return;
        }
        if (!"ADMIN".equals(principal.type())) {
            writeUnauthorized(response);
            return;
        }
        LoginContext.set(principal);
        try {
            filterChain.doFilter(request, response);
        } finally {
            LoginContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.fail(ErrorCode.TOKEN_INVALID.getCode(),
                        ErrorCode.TOKEN_INVALID.getMessage()));
    }
}
