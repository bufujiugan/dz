package com.dz.tavern.service.aspect;

import com.dz.tavern.common.annotation.Idempotent;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.dao.mapper.IdempotentRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotentAspect {
    private final IdempotentRecordMapper idempotentRecordMapper;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String businessKey = resolveKey(joinPoint, idempotent.key());
        String methodName = joinPoint.getSignature().toShortString();
        String idempotentKey = methodName + ":" + businessKey;
        int affected = idempotentRecordMapper.tryAcquire(idempotentKey,
                LocalDateTime.now().plusSeconds(idempotent.lockSeconds()));
        if (affected == 0) {
            // 已成功的 void 操作直接返回；处理中或失败未过期的请求拒绝并发重入。
            if ("SUCCESS".equals(idempotentRecordMapper.selectStatus(idempotentKey))
                    && ((MethodSignature) joinPoint.getSignature()).getReturnType() == Void.TYPE) {
                log.info("幂等请求已成功，忽略重复调用 method={} businessKey={}",
                        methodName, businessKey);
                return null;
            }
            log.warn("幂等请求冲突 method={} businessKey={}", methodName, businessKey);
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT);
        }
        // 业务异常时保留 PROCESSING 到锁过期，防止失败瞬间被并发请求立即重放。
        Object result = joinPoint.proceed();
        idempotentRecordMapper.markSuccess(idempotentKey);
        return result;
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String expression) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();
        Object[] arguments = joinPoint.getArgs();
        if (parameterNames != null) {
            for (int index = 0; index < parameterNames.length; index++) {
                context.setVariable(parameterNames[index], arguments[index]);
            }
        }
        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value == null) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        return String.valueOf(value);
    }
}
