package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 型式批准影响评估状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum TypeApprovalAssessmentState {

    NOT_ASSESSED("未评估", 0),
    PASSED("通过", 1),
    BLOCKED("阻断", 2);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static TypeApprovalAssessmentState valOf(Integer val) {
        return Arrays.stream(TypeApprovalAssessmentState.values())
                .filter(state -> state.value == val)
                .findFirst()
                .orElse(null);
    }

}
