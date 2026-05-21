package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 适配级别枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum AdaptiveLevel {

    LE("基础版本及以下", 1),
    GE("基础版本及以上", 2),
    EQ("与基础版本一致", 3);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static AdaptiveLevel valOf(Integer val) {
        return Arrays.stream(AdaptiveLevel.values())
                .filter(adaptiveLevel -> adaptiveLevel.value == val)
                .findFirst()
                .orElse(null);
    }

}
