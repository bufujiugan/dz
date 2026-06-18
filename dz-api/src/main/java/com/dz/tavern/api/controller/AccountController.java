package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.AccountLogEntity;
import com.dz.tavern.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/logs")
    public ApiResponse<PageResult<AccountLogEntity>> logs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        YearMonth yearMonth = month == null || month.isBlank() ? null : YearMonth.parse(month);
        return ApiResponse.ok(accountService.pageLogs(
                LoginContext.currentId(), type, yearMonth, null, current, size));
    }
}
