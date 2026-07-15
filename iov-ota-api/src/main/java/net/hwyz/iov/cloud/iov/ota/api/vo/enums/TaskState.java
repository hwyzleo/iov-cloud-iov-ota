package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 升级任务状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum TaskState {

    PENDING("待提交", 1),
    SUBMITTED("待审核", 2),
    APPROVED("已审核", 3),
    REJECTED("未通过", 4),
    RELEASED("已发布", 5),
    PAUSED("已暂停", 6),
    FINISHED("已结束", 7),
    CANCELLED("已取消", 8);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static TaskState valOf(Integer val) {
        if (val == null) {
            return null;
        }
        return Arrays.stream(TaskState.values())
                .filter(taskState -> taskState.value == val)
                .findFirst()
                .orElse(null);
    }

}
