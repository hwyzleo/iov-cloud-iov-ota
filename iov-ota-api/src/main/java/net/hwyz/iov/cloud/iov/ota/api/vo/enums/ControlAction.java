package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 云端控制动作枚举类（CR-012 §5.6、§7）
 *
 * <p>云端控制是带作用域和安全应用模式的意图，车端保留本地安全裁决权。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ControlAction {

    NONE("无"),
    CONTINUE("继续"),
    PAUSE("暂停"),
    ABORT("中止"),
    ROLLBACK("回滚"),
    RESYNC("重新同步");

    public final String label;
    public final String value = name();

    public static ControlAction valOf(String val) {
        if (val == null) {
            return null;
        }
        for (ControlAction action : values()) {
            if (action.value.equals(val)) {
                return action;
            }
        }
        return null;
    }
}
