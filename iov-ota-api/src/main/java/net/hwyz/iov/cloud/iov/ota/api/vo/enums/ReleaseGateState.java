package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 多任务放行门禁状态枚举类（CR-015）
 * <p>对应 tb_task_release_gate.gate_state</p>
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ReleaseGateState {

    PASS("放行", "PASS"),
    FAIL("拦截", "FAIL"),
    PENDING("待定", "PENDING");

    public final String label;
    public final String value;

    public static ReleaseGateState valOf(String val) {
        for (ReleaseGateState state : values()) {
            if (state.value.equals(val)) {
                return state;
            }
        }
        return null;
    }
}
