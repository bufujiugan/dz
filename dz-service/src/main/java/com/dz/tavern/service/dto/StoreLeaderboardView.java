package com.dz.tavern.service.dto;

import java.util.List;

public record StoreLeaderboardView(
        Long storeId,
        List<StorePointsEntry> top20,
        StorePointsEntry mine) {
}
