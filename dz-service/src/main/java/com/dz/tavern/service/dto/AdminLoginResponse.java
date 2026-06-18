package com.dz.tavern.service.dto;

import java.util.Set;

public record AdminLoginResponse(String token, String realName, Set<String> permissions) {
}
