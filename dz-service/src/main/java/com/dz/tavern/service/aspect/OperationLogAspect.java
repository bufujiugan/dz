package com.dz.tavern.service.aspect;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.dao.entity.OperationLogEntity;
import com.dz.tavern.dao.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private final OperationLogMapper operationLogMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            // 审计日志写入失败不能覆盖原业务结果，因此在 finally 中独立捕获并告警。
            try {
                OperationLogEntity operationLog = new OperationLogEntity();
                operationLog.setAdminId(LoginContext.currentId());
                operationLog.setModule(opLog.module());
                operationLog.setAction(opLog.action());
                operationLog.setParamsDigest(digest(joinPoint));
                operationLog.setIp(resolveIp());
                operationLog.setCostMs((System.nanoTime() - start) / 1_000_000);
                operationLogMapper.insertOperationLog(operationLog);
            } catch (Exception exception) {
                log.error("后台操作日志写入失败 module={} action={}",
                        opLog.module(), opLog.action(), exception);
            }
        }
    }

    private String digest(ProceedingJoinPoint joinPoint) throws NoSuchAlgorithmException {
        // 摘要只包含方法签名和参数类型，不持久化密码、token 或请求正文。
        StringBuilder source = new StringBuilder(joinPoint.getSignature().toLongString());
        for (Object argument : joinPoint.getArgs()) {
            source.append('|').append(argument == null ? "null" : argument.getClass().getName());
        }
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(source.toString().getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private String resolveIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            return forwarded == null || forwarded.isBlank()
                    ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        }
        return "unknown";
    }
}
