package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

/**
 * 用户授权结果枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum ConsentResult {

    DENIED("拒绝", 0),
    GRANTED("同意", 1);

    public final String label;
    public final int value;

    public static ConsentResult valOf(Integer val) {
        if (val == null) {
            return null;
        }
        for (ConsentResult result : values()) {
            if (result.value == val) {
                return result;
            }
        }
        return null;
    }
}
