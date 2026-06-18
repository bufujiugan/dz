package com.dz.tavern.common.context;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;

public final class LoginContext {
    private static final ThreadLocal<AuthPrincipal> HOLDER = new ThreadLocal<>();

    private LoginContext() {
    }

    public static void set(AuthPrincipal principal) {
        HOLDER.set(principal);
    }

    public static AuthPrincipal get() {
        AuthPrincipal principal = HOLDER.get();
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }

    public static Long currentId() {
        return get().id();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
