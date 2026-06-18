package com.dz.tavern.service.dto;

public record StorePointsEntry(
        Long rank,
        Long userId,
        String nickname,
        String avatar,
        Long points) {
}
