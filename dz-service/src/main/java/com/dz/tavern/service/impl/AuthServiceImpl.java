package com.dz.tavern.service.impl;

import com.dz.tavern.common.util.JwtUtil;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.dao.entity.StoreEntity;
import com.dz.tavern.dao.entity.UserAccountEntity;
import com.dz.tavern.dao.entity.UserEntity;
import com.dz.tavern.dao.mapper.StoreMapper;
import com.dz.tavern.dao.mapper.UserAccountMapper;
import com.dz.tavern.dao.mapper.UserMapper;
import com.dz.tavern.service.AuthService;
import com.dz.tavern.service.UserService;
import com.dz.tavern.service.dto.LoginResponse;
import com.dz.tavern.service.dto.MiniProgramAuthorizeRequest;
import com.dz.tavern.service.wechat.WechatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private static final Duration USER_TOKEN_DURATION = Duration.ofDays(7);
    private static final String DEFAULT_WECHAT_NICKNAME = "微信用户";
    private static final String[] DEFAULT_NICKNAMES = {
            "微醺旅人",
            "夜杯客",
            "吟游诗人",
            "暗巷幽灵",
            "远航船客",
            "烛影酒友",
            "铜舵客",
            "月下黑猫"
    };
    private static final String[] DEFAULT_AVATARS = {
            "/uploads/default-avatars/tavern-01.png",
            "/uploads/default-avatars/tavern-02.png",
            "/uploads/default-avatars/tavern-03.png",
            "/uploads/default-avatars/tavern-04.png",
            "/uploads/default-avatars/tavern-05.png",
            "/uploads/default-avatars/tavern-06.png",
            "/uploads/default-avatars/tavern-07.png",
            "/uploads/default-avatars/tavern-08.png"
    };

    private final WechatClient wechatClient;
    private final UserMapper userMapper;
    private final UserAccountMapper userAccountMapper;
    private final StoreMapper storeMapper;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public LoginResponse login(String code, Long storeId) {
        requireActiveStore(storeId);
        log.info("开始处理微信登录 storeId={}", storeId);
        WechatClient.SessionResult session = wechatClient.code2Session(code);
        UserEntity user = userMapper.selectByStoreIdAndOpenid(storeId, session.openid());
        if (user == null) {
            DefaultProfile defaultProfile = buildDefaultProfile(storeId, session.openid());
            user = createWechatUser(storeId, session, defaultProfile.nickname(),
                    defaultProfile.avatar(), null);
        } else {
            user = ensureDefaultProfile(user);
        }
        log.info("微信登录成功 userId={} storeId={}", user.getId(), storeId);
        return createLoginResponse(user);
    }

    @Override
    @Transactional
    public LoginResponse authorizeMiniProgram(MiniProgramAuthorizeRequest request) {
        Long storeId = request.storeId();
        requireActiveStore(storeId);
        log.info("开始处理微信小程序授权 storeId={}", storeId);
        WechatClient.SessionResult session = wechatClient.code2Session(request.loginCode());
        String phone = resolveAuthorizedPhone(request.phoneCode());
        UserEntity user = userMapper.selectByStoreIdAndOpenid(storeId, session.openid());
        if (phone != null) {
            ensurePhoneCanBind(user == null ? null : user.getId(), storeId, phone);
        }
        if (user == null) {
            user = createWechatUser(storeId, session,
                    resolveNickname(null, storeId, session.openid(), request.nickname()),
                    resolveAvatar(null, storeId, session.openid(), request.avatar()), phone);
        } else {
            userMapper.updateWechatAuthorization(user.getId(), session.unionid(),
                    resolveNickname(user, storeId, session.openid(), request.nickname()),
                    resolveAvatar(user, storeId, session.openid(), request.avatar()), phone);
            user = userMapper.selectById(user.getId());
        }
        log.info("微信小程序授权成功 userId={} storeId={} phoneBound={}",
                user.getId(), storeId, phone != null);
        return createLoginResponse(user);
    }

    @Override
    @Transactional
    public void bindPhone(Long userId, String code) {
        UserEntity user = requireUser(userId);
        String phone = wechatClient.getPhoneNumber(code);
        ensurePhoneCanBind(userId, user.getStoreId(), phone);
        userMapper.updatePhone(userId, phone);
        log.info("用户手机号绑定完成 userId={} storeId={}", userId, user.getStoreId());
    }

    private UserEntity createWechatUser(Long storeId, WechatClient.SessionResult session,
                                        String nickname, String avatar, String phone) {
        UserEntity user = new UserEntity();
        user.setStoreId(storeId);
        user.setOpenid(session.openid());
        user.setUnionid(session.unionid());
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setPhone(phone);
        user.setStatus(0);
        userMapper.insertUser(user);

        UserAccountEntity account = new UserAccountEntity();
        account.setUserId(user.getId());
        account.setBalanceFen(0L);
        account.setPoints(0L);
        account.setFrozenPoints(0L);
        account.setVersion(0);
        userAccountMapper.insertAccount(account);
        log.info("微信登录创建新用户 userId={} storeId={}", user.getId(), storeId);
        return user;
    }

    private void ensurePhoneCanBind(Long userId, Long storeId, String phone) {
        UserEntity owner = userMapper.selectByStoreIdAndPhone(storeId, phone);
        if (owner != null && !owner.getId().equals(userId)) {
            log.warn("手机号绑定冲突 userId={} storeId={} occupiedUserId={}",
                    userId, storeId, owner.getId());
            throw new BizException(ErrorCode.PHONE_ALREADY_BOUND);
        }
    }

    private String resolveAuthorizedPhone(String phoneCode) {
        if (phoneCode == null || phoneCode.isBlank()) {
            return null;
        }
        return wechatClient.getPhoneNumber(phoneCode);
    }

    private LoginResponse createLoginResponse(UserEntity user) {
        String token = jwtUtil.createToken(user.getId(), user.getOpenid(), "USER",
                Set.of(), USER_TOKEN_DURATION);
        long expireAt = System.currentTimeMillis() + USER_TOKEN_DURATION.toMillis();
        return new LoginResponse(token, expireAt, userService.getProfile(user.getId()));
    }

    private UserEntity ensureDefaultProfile(UserEntity user) {
        boolean nicknameMissing = shouldUseDefaultNickname(user.getNickname());
        boolean avatarMissing = user.getAvatar() == null || user.getAvatar().isBlank();
        if (!nicknameMissing && !avatarMissing) {
            return user;
        }
        DefaultProfile defaultProfile = buildDefaultProfile(user.getStoreId(), user.getOpenid());
        String nickname = nicknameMissing ? defaultProfile.nickname() : user.getNickname();
        String avatar = avatarMissing ? defaultProfile.avatar() : user.getAvatar();
        userMapper.updateProfile(user.getId(), nickname, avatar);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        log.info("用户默认资料已补齐 userId={} storeId={} nicknameDefaulted={} avatarDefaulted={}",
                user.getId(), user.getStoreId(), nicknameMissing, avatarMissing);
        return user;
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return user;
    }

    private void requireActiveStore(Long storeId) {
        if (storeId == null) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        StoreEntity store = storeMapper.selectById(storeId);
        if (store == null || store.getStatus() == null || store.getStatus() != 1) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
    }

    private String resolveNickname(UserEntity user, Long storeId, String openid, String nickname) {
        if (nickname != null && !nickname.isBlank()
                && !DEFAULT_WECHAT_NICKNAME.equals(nickname.trim())) {
            return nickname.trim();
        }
        if (user != null && !shouldUseDefaultNickname(user.getNickname())) {
            return user.getNickname();
        }
        return buildDefaultProfile(storeId, openid).nickname();
    }

    private String resolveAvatar(UserEntity user, Long storeId, String openid, String avatar) {
        if (avatar != null && !avatar.isBlank()) {
            return avatar.trim();
        }
        if (user != null && user.getAvatar() != null && !user.getAvatar().isBlank()) {
            return user.getAvatar();
        }
        return buildDefaultProfile(storeId, openid).avatar();
    }

    private boolean shouldUseDefaultNickname(String nickname) {
        return nickname == null || nickname.isBlank()
                || DEFAULT_WECHAT_NICKNAME.equals(nickname.trim());
    }

    private DefaultProfile buildDefaultProfile(Long storeId, String openid) {
        String seed = storeId + ":" + (openid == null ? "" : openid);
        int index = Math.floorMod(seed.hashCode(), DEFAULT_NICKNAMES.length);
        return new DefaultProfile(DEFAULT_NICKNAMES[index],
                DEFAULT_AVATARS[index % DEFAULT_AVATARS.length]);
    }

    private record DefaultProfile(String nickname, String avatar) {
    }
}
