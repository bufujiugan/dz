package com.dz.tavern.service.dto;

public record UserProfile(Long userId, String nickname, String avatar, String phone,
                          Long balanceFen, Long points, Long frozenPoints, Integer status) {
}
