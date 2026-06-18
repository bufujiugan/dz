package com.dz.tavern.service;

import com.dz.tavern.service.dto.AdminLoginResponse;

public interface AdminAuthService {
    AdminLoginResponse login(String username, String password);
}
