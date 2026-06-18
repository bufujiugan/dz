package com.dz.tavern.dao.projection;

import lombok.Data;

@Data
public class PointsRankingRow {
    private Long userId;
    private String nickname;
    private String avatar;
    private Long points;
}
