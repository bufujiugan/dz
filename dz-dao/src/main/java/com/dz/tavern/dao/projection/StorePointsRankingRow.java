package com.dz.tavern.dao.projection;

import lombok.Data;

@Data
public class StorePointsRankingRow {
    private Long userId;
    private String nickname;
    private String avatar;
    private Long points;
    private Long rank;
}
