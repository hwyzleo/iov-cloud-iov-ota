package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 升级任务类型枚举类
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum TaskType {

    NORMAL(1, "普通任务"),
    LIGHT(2, "快速任务");

    private final int value;
    private final String label;

    public static TaskType valOf(Integer val) {
        return Arrays.stream(TaskType.values())
                .filter(taskType -> taskType.value == val)
                .findFirst()
                .orElse(null);
    }

}
