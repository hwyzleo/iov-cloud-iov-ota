package net.hwyz.iov.cloud.iov.ota.api.contract.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 升级任务限制条件类型枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum TaskRestrictionType {

    BASELINE_EXCLUDE("过滤排除的基线号"),
    BASELINE_UNIFICATION("未对齐基线的车辆拉齐基线"),
    ADAPTATION_SUBJECT("适配主体"),
    COMPARISON_CRITERIA("比对基准");

    /**
     * 名称
     */
    public final String label;

}
