package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

/**
 * 门禁状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum GateState {

    OK("正常", "OK"),
    BREACH("越阈值", "BREACH");

    public final String label;
    public final String value;

    public static GateState valOf(String val) {
        for (GateState state : values()) {
            if (state.value.equals(val)) {
                return state;
            }
        }
        return null;
    }
}
