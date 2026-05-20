package net.hwyz.iov.cloud.iov.ota.service.domain.contract.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 升级模式参数枚举类
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum UpgradeModeArg {

    SCHEDULED_TIME("计划升级时间");

    private final String label;

}
