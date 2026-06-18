package com.dz.tavern.common.exception;

import com.dz.tavern.common.model.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException exception) {
        HttpStatus status = exception.getCode() == ErrorCode.UNAUTHORIZED.getCode()
                ? HttpStatus.UNAUTHORIZED : HttpStatus.OK;
        if (status == HttpStatus.UNAUTHORIZED
                || exception.getCode() == ErrorCode.FORBIDDEN.getCode()) {
            log.warn("安全校验未通过 code={} message={}",
                    exception.getCode(), exception.getMessage(), exception);
        }
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            ConstraintViolationException.class})
    public ApiResponse<Void> handleValidationException(Exception exception) {
        log.warn("请求参数校验失败 exceptionType={}",
                exception.getClass().getSimpleName(), exception);
        return ApiResponse.fail(ErrorCode.INVALID_PARAMETER.getCode(),
                ErrorCode.INVALID_PARAMETER.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("未处理的系统异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.SYSTEM_ERROR.getCode(),
                        ErrorCode.SYSTEM_ERROR.getMessage()));
    }
}
