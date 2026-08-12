package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 控制回执状态枚举类（CR-012 §5.6、§7）
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ControlAckStatus {

    RECEIVED("已接收"),
    DEFERRED("已延期"),
    APPLIED("已应用"),
    REJECTED("已拒绝");

    public final String label;
    public final String value = name();

    public static ControlAckStatus valOf(String val) {
        if (val == null) {
            return null;
        }
        for (ControlAckStatus status : values()) {
            if (status.value.equals(val)) {
                return status;
            }
        }
        return null;
    }
}
