package com.dz.tavern.service.dto;

public record LoginResponse(String token, Long expireAt, UserProfile user) {
}
