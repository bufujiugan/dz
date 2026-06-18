package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.StorePointsLogEntity;
import com.dz.tavern.dao.entity.StorePointsRequestEntity;
import com.dz.tavern.dao.entity.UserStorePointsEntity;
import com.dz.tavern.dao.projection.StorePointsRankingRow;
import com.dz.tavern.dao.projection.StorePointsUserSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StorePointsMapper {
    UserStorePointsEntity selectAccount(@Param("userId") Long userId,
                                        @Param("storeId") Long storeId);

    List<UserStorePointsEntity> selectStoreAccountsForUpdate(@Param("storeId") Long storeId);

    int insertAccount(UserStorePointsEntity account);

    int updateAccountOptimistic(@Param("userId") Long userId,
                                @Param("storeId") Long storeId,
                                @Param("points") Long points,
                                @Param("frozenPoints") Long frozenPoints,
                                @Param("version") Integer version);

    List<StorePointsRankingRow> selectRanking(@Param("storeId") Long storeId,
                                              @Param("limit") int limit);

    StorePointsRankingRow selectUserRanking(@Param("storeId") Long storeId,
                                            @Param("userId") Long userId);

    List<StorePointsUserSummaryRow> selectUserSummaries(
            @Param("storeId") Long storeId,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("size") long size);

    long countUserSummaries(@Param("storeId") Long storeId,
                            @Param("keyword") String keyword);

    int insertRequest(StorePointsRequestEntity request);

    StorePointsRequestEntity selectRequestById(@Param("id") Long id);

    List<StorePointsRequestEntity> selectRequests(
            @Param("storeId") Long storeId,
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("offset") long offset,
            @Param("size") long size);

    long countRequests(@Param("storeId") Long storeId,
                       @Param("userId") Long userId,
                       @Param("status") String status);

    int updateRequestAudit(StorePointsRequestEntity request);

    int insertLog(StorePointsLogEntity log);

    List<StorePointsLogEntity> selectLogs(
            @Param("storeId") Long storeId,
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("size") long size);

    long countLogs(@Param("storeId") Long storeId,
                   @Param("userId") Long userId);
}
