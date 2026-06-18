package com.dz.tavern.service.impl;

import com.dz.tavern.common.enums.AccountChangeType;
import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.AccountLogEntity;
import com.dz.tavern.service.AccountService;
import com.dz.tavern.service.AdminAccountService;
import com.dz.tavern.service.dto.AccountAdjustCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAccountServiceImpl implements AdminAccountService {
    private final AccountService accountService;

    @Override
    public void adjust(AccountAdjustCommand command, Long adminId) {
        if (!command.confirm() || command.value() == 0) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        String bizNo = "ADMIN_ADJUST_" + adminId + "_" + System.currentTimeMillis();
        log.info("管理员开始调整用户资产 adminId={} userId={} assetType={} value={} bizNo={}",
                adminId, command.userId(), command.type(), command.value(), bizNo);
        if (command.type() == AccountAdjustCommand.AssetType.BALANCE) {
            accountService.changeBalance(command.userId(), AccountChangeType.ADMIN_ADJUST,
                    command.value(), bizNo, "ADMIN:" + adminId, command.remark());
        } else {
            accountService.changePoints(command.userId(), AccountChangeType.ADMIN_ADJUST,
                    command.value(), bizNo, "ADMIN:" + adminId, command.remark());
        }
        log.info("管理员调整用户资产完成 adminId={} userId={} assetType={} bizNo={}",
                adminId, command.userId(), command.type(), bizNo);
    }

    @Override
    public String exportCsv(Long storeId, Long userId, String changeType, String bizNo) {
        StringBuilder csv = new StringBuilder(
                "id,userId,assetType,changeType,changeValue,beforeValue,afterValue,bizNo,operator,remark\n");
        long current = 1;
        while (true) {
            PageResult<AccountLogEntity> page = accountService.pageLogs(
                    storeId, userId, changeType, null, bizNo, current, 50);
            for (AccountLogEntity log : page.records()) {
                StringJoiner row = new StringJoiner(",");
                row.add(String.valueOf(log.getId()));
                row.add(String.valueOf(log.getUserId()));
                row.add(csvValue(log.getAssetType()));
                row.add(csvValue(log.getChangeType()));
                row.add(String.valueOf(log.getChangeValue()));
                row.add(String.valueOf(log.getBeforeValue()));
                row.add(String.valueOf(log.getAfterValue()));
                row.add(csvValue(log.getBizNo()));
                row.add(csvValue(log.getOperator()));
                row.add(csvValue(log.getRemark()));
                csv.append(row).append('\n');
            }
            if (current * page.size() >= page.total()) {
                break;
            }
            current++;
        }
        return csv.toString();
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
