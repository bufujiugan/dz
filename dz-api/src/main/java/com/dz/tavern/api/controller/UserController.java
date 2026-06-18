package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.service.UserService;
import com.dz.tavern.service.dto.ProfileUpdateRequest;
import com.dz.tavern.service.dto.LeaderboardEntry;
import com.dz.tavern.service.dto.UserProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public ApiResponse<UserProfile> profile() {
        return ApiResponse.ok(userService.getProfile(LoginContext.currentId()));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        userService.updateProfile(LoginContext.currentId(), request);
        return ApiResponse.ok();
    }

    @GetMapping("/leaderboard")
    public ApiResponse<List<LeaderboardEntry>> leaderboard() {
        return ApiResponse.ok(userService.pointsLeaderboard(20));
    }
}
