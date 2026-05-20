package net.hwyz.iov.cloud.iov.ota.api.contract.enums;

import lombok.AllArgsConstructor;

/**
 * 升级任务策略类型枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum TaskStrategyType {

    ROLLBACK("是否回滚"),
    FLASH_COUNT("刷写次数"),
    IMPACT_VEHICLE_OPERATION("用车影响"),
    KEEP_IN_PARK("保持驻车(P档)"),
    NOT_CHARGING("不在充电"),
    NO_EXTERNAL_POWER("不对外供电"),
    ALL_CLOSED("车窗、天窗、车门及尾门关闭"),
    HV_SOC("高压电量"),
    LV_SOC("低压电量");

    /**
     * 名称
     */
    public final String label;

}
