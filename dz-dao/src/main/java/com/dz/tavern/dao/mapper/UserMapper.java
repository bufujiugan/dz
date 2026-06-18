package com.dz.tavern.dao.mapper;

import com.dz.tavern.dao.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    UserEntity selectById(@Param("id") Long id);

    UserEntity selectByStoreIdAndOpenid(@Param("storeId") Long storeId,
                                        @Param("openid") String openid);

    UserEntity selectByStoreIdAndPhone(@Param("storeId") Long storeId,
                                       @Param("phone") String phone);

    int insertUser(UserEntity user);

    int updatePhone(@Param("userId") Long userId, @Param("phone") String phone);

    int updateWechatAuthorization(@Param("userId") Long userId,
                                  @Param("unionid") String unionid,
                                  @Param("nickname") String nickname,
                                  @Param("avatar") String avatar,
                                  @Param("phone") String phone);

    int updateProfile(@Param("userId") Long userId, @Param("nickname") String nickname,
                      @Param("avatar") String avatar);

    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

    List<UserEntity> selectPage(@Param("storeId") Long storeId,
                                @Param("keyword") String keyword,
                                @Param("offset") long offset, @Param("size") long size);

    long countPage(@Param("storeId") Long storeId,
                   @Param("keyword") String keyword);
}
