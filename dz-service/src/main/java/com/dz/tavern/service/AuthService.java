package com.dz.tavern.service;

import com.dz.tavern.service.dto.LoginResponse;
import com.dz.tavern.service.dto.MiniProgramAuthorizeRequest;

public interface AuthService {
    LoginResponse login(String code, Long storeId);

    LoginResponse authorizeMiniProgram(MiniProgramAuthorizeRequest request);

    void bindPhone(Long userId, String code);
}
