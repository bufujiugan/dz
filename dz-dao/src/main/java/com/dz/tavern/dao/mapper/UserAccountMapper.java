package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.AccountLogEntity;
import com.dz.tavern.dao.entity.UserAccountEntity;
import com.dz.tavern.dao.projection.PointsRankingRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserAccountMapper {
    UserAccountEntity selectByUserId(@Param("userId") Long userId);

    List<UserAccountEntity> selectAllForUpdate();

    List<PointsRankingRow> selectPointsRanking(@Param("limit") int limit);

    int insertAccount(UserAccountEntity account);

    int updateAccountOptimistic(@Param("userId") Long userId,
                                @Param("balanceFen") Long balanceFen,
                                @Param("points") Long points,
                                @Param("frozenPoints") Long frozenPoints,
                                @Param("version") Integer version);

    int insertAccountLog(AccountLogEntity accountLog);

    List<AccountLogEntity> selectLogs(@Param("storeId") Long storeId,
                                      @Param("userId") Long userId,
                                      @Param("changeType") String changeType,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      @Param("bizNo") String bizNo,
                                      @Param("offset") long offset,
                                      @Param("size") long size);

    long countLogs(@Param("storeId") Long storeId,
                   @Param("userId") Long userId,
                   @Param("changeType") String changeType,
                   @Param("startTime") LocalDateTime startTime,
                   @Param("endTime") LocalDateTime endTime,
                   @Param("bizNo") String bizNo);
}
