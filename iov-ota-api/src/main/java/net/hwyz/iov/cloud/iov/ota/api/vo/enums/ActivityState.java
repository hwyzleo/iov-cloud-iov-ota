package net.hwyz.iov.cloud.iov.ota.api.contract.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 升级活动状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum ActivityState {

    PENDING("待提交", 1),
    SUBMITTED("待审核", 2),
    APPROVED("已审核", 3),
    REJECTED("未通过", 4),
    RELEASED("已发布", 5),
    FINISHED("已结束", 6),
    CANCELLED("已取消", 7);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static ActivityState valOf(Integer val) {
        return Arrays.stream(ActivityState.values())
                .filter(activityState -> activityState.value == val)
                .findFirst()
                .orElse(null);
    }

}
