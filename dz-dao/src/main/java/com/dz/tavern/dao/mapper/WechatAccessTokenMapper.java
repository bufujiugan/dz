package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.WechatAccessTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WechatAccessTokenMapper {
    WechatAccessTokenEntity selectByTokenType(@Param("tokenType") String tokenType);

    int upsert(WechatAccessTokenEntity accessToken);
}
