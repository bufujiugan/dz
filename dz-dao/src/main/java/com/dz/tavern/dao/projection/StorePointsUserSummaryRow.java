package com.dz.tavern.dao.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StorePointsUserSummaryRow {
    private Long userId;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer status;
    private Long totalPoints;
    private Long frozenPoints;
    private Integer storeCount;
    private LocalDateTime lastChangeTime;
}
