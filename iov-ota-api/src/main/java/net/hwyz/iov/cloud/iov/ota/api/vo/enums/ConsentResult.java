package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 授权业务结果枚举类（CR-016 §3.2）
 *
 * <p>不可变授权历史记录中的业务结果：GRANTED/REJECTED/REVOKED。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ConsentResult {

    GRANTED("同意"),
    REJECTED("拒绝"),
    REVOKED("撤回");

    public final String label;
    public final String value = name();

    public static ConsentResult valOf(String val) {
        if (val == null) {
            return null;
        }
        for (ConsentResult result : values()) {
            if (result.value.equals(val)) {
                return result;
            }
        }
        return null;
    }
}
