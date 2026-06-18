package com.dz.tavern.service.dto;

public record LeaderboardEntry(
        Integer rank,
        Long userId,
        String nickname,
        String avatar,
        Long points) {
}
