package com.dz.tavern.service;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.util.JwtUtil;
import com.dz.tavern.dao.entity.StoreEntity;
import com.dz.tavern.dao.entity.UserAccountEntity;
import com.dz.tavern.dao.entity.UserEntity;
import com.dz.tavern.dao.mapper.StoreMapper;
import com.dz.tavern.dao.mapper.UserAccountMapper;
import com.dz.tavern.dao.mapper.UserMapper;
import com.dz.tavern.service.dto.LoginResponse;
import com.dz.tavern.service.dto.MiniProgramAuthorizeRequest;
import com.dz.tavern.service.dto.UserProfile;
import com.dz.tavern.service.impl.AuthServiceImpl;
import com.dz.tavern.service.wechat.WechatClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {
    private static final long STORE_ID = 10001L;

    @Test
    void authorizeMiniProgramShouldCreateUserWithProfileAndPhone() {
        WechatClient wechatClient = mock(WechatClient.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        UserService userService = mock(UserService.class);
        AuthService service = new AuthServiceImpl(
                wechatClient, userMapper, userAccountMapper, storeMapper, userService, jwtUtil());

        when(storeMapper.selectById(STORE_ID)).thenReturn(activeStore());
        when(wechatClient.code2Session("login-code"))
                .thenReturn(new WechatClient.SessionResult("openid-1", "unionid-1"));
        when(wechatClient.getPhoneNumber("phone-code")).thenReturn("13800138000");
        when(userMapper.selectByStoreIdAndOpenid(STORE_ID, "openid-1")).thenReturn(null);
        when(userMapper.selectByStoreIdAndPhone(STORE_ID, "13800138000")).thenReturn(null);
        when(userMapper.insertUser(any())).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });
        when(userService.getProfile(1L)).thenReturn(new UserProfile(
                1L, "点单会员", "https://example.com/avatar.png",
                "138****8000", 0L, 0L, 0L, 0));

        LoginResponse response = service.authorizeMiniProgram(new MiniProgramAuthorizeRequest(
                "login-code", STORE_ID, "phone-code", "点单会员",
                "https://example.com/avatar.png"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().nickname()).isEqualTo("点单会员");
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insertUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getStoreId()).isEqualTo(STORE_ID);
        assertThat(userCaptor.getValue().getOpenid()).isEqualTo("openid-1");
        assertThat(userCaptor.getValue().getUnionid()).isEqualTo("unionid-1");
        assertThat(userCaptor.getValue().getPhone()).isEqualTo("13800138000");
        ArgumentCaptor<UserAccountEntity> accountCaptor =
                ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountMapper).insertAccount(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getUserId()).isEqualTo(1L);
    }

    @Test
    void authorizeMiniProgramShouldRejectOccupiedPhone() {
        WechatClient wechatClient = mock(WechatClient.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        UserService userService = mock(UserService.class);
        AuthService service = new AuthServiceImpl(
                wechatClient, userMapper, userAccountMapper, storeMapper, userService, jwtUtil());

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);
        currentUser.setStoreId(STORE_ID);
        currentUser.setOpenid("openid-1");
        UserEntity phoneOwner = new UserEntity();
        phoneOwner.setId(2L);
        when(storeMapper.selectById(STORE_ID)).thenReturn(activeStore());
        when(wechatClient.code2Session("login-code"))
                .thenReturn(new WechatClient.SessionResult("openid-1", "unionid-1"));
        when(wechatClient.getPhoneNumber("phone-code")).thenReturn("13800138000");
        when(userMapper.selectByStoreIdAndOpenid(STORE_ID, "openid-1")).thenReturn(currentUser);
        when(userMapper.selectByStoreIdAndPhone(STORE_ID, "13800138000")).thenReturn(phoneOwner);

        assertThatThrownBy(() -> service.authorizeMiniProgram(new MiniProgramAuthorizeRequest(
                "login-code", STORE_ID, "phone-code", "点单会员", "")))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PHONE_ALREADY_BOUND.getCode());
        verify(userMapper, never()).updateWechatAuthorization(any(), any(), any(), any(), any());
        verify(userAccountMapper, never()).insertAccount(any());
    }

    @Test
    void authorizeMiniProgramShouldUpdateProfileWithoutPhoneCode() {
        WechatClient wechatClient = mock(WechatClient.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        UserService userService = mock(UserService.class);
        AuthService service = new AuthServiceImpl(
                wechatClient, userMapper, userAccountMapper, storeMapper, userService, jwtUtil());

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);
        currentUser.setStoreId(STORE_ID);
        currentUser.setOpenid("openid-1");
        when(storeMapper.selectById(STORE_ID)).thenReturn(activeStore());
        when(wechatClient.code2Session("login-code"))
                .thenReturn(new WechatClient.SessionResult("openid-1", "unionid-1"));
        when(userMapper.selectByStoreIdAndOpenid(STORE_ID, "openid-1")).thenReturn(currentUser);
        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(userService.getProfile(1L)).thenReturn(new UserProfile(
                1L, "点单会员", "https://example.com/avatar.png",
                "138****8000", 0L, 0L, 0L, 0));

        LoginResponse response = service.authorizeMiniProgram(new MiniProgramAuthorizeRequest(
                "login-code", STORE_ID, null, "点单会员",
                "https://example.com/avatar.png"));

        assertThat(response.user().nickname()).isEqualTo("点单会员");
        verify(userMapper).updateWechatAuthorization(
                1L, "unionid-1", "点单会员",
                "https://example.com/avatar.png", null);
        verify(wechatClient, never()).getPhoneNumber(any());
        verify(userMapper, never()).selectByStoreIdAndPhone(any(), any());
        verify(userAccountMapper, never()).insertAccount(any());
    }

    @Test
    void authorizeMiniProgramShouldKeepExistingProfileWhenWechatReturnsAnonymousProfile() {
        WechatClient wechatClient = mock(WechatClient.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        UserService userService = mock(UserService.class);
        AuthService service = new AuthServiceImpl(
                wechatClient, userMapper, userAccountMapper, storeMapper, userService, jwtUtil());

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);
        currentUser.setStoreId(STORE_ID);
        currentUser.setOpenid("openid-1");
        currentUser.setNickname("Real Member");
        currentUser.setAvatar("https://example.com/real.png");
        when(storeMapper.selectById(STORE_ID)).thenReturn(activeStore());
        when(wechatClient.code2Session("login-code"))
                .thenReturn(new WechatClient.SessionResult("openid-1", "unionid-1"));
        when(userMapper.selectByStoreIdAndOpenid(STORE_ID, "openid-1")).thenReturn(currentUser);
        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(userService.getProfile(1L)).thenReturn(new UserProfile(
                1L, "Real Member", "https://example.com/real.png",
                "138****8000", 0L, 0L, 0L, 0));

        LoginResponse response = service.authorizeMiniProgram(new MiniProgramAuthorizeRequest(
                "login-code", STORE_ID, null, "微信用户", ""));

        assertThat(response.user().nickname()).isEqualTo("Real Member");
        verify(userMapper).updateWechatAuthorization(
                1L, "unionid-1", "Real Member",
                "https://example.com/real.png", null);
        verify(wechatClient, never()).getPhoneNumber(any());
        verify(userAccountMapper, never()).insertAccount(any());
    }

    private JwtUtil jwtUtil() {
        return new JwtUtil("0123456789abcdef0123456789abcdef");
    }

    private StoreEntity activeStore() {
        StoreEntity store = new StoreEntity();
        store.setId(STORE_ID);
        store.setStatus(1);
        return store;
    }
}
