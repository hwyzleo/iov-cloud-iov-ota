package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 升级任务阶段枚举类
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum TaskPhase {

    VALIDATION(1, "验证阶段"),
    CANARY(2, "灰度阶段"),
    RELEASE(3, "发布阶段");

    private final int value;
    private final String label;

    public static TaskPhase valOf(Integer val) {
        return Arrays.stream(TaskPhase.values())
                .filter(taskPhase -> taskPhase.value == val)
                .findFirst()
                .orElse(null);
    }

}
