package com.dz.tavern.common.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BizNoGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private BizNoGenerator() {
    }

    public static String orderNo() {
        return "O" + LocalDateTime.now().format(FORMATTER) + randomDigits(6);
    }

    public static String rechargeNo() {
        return "R" + LocalDateTime.now().format(FORMATTER) + randomDigits(6);
    }

    private static String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
