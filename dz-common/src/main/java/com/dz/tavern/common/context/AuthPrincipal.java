package com.dz.tavern.common.context;

import java.util.Set;

public record AuthPrincipal(Long id, String subject, String type, Set<String> permissions) {
}
