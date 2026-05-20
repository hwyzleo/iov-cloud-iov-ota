package net.hwyz.iov.cloud.iov.ota.api.contract.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 适配主体枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum AdaptiveSubject {

    SOFTWARE_PN("软件零件号", 1),
    SOFTWARE_BUILD_VERSION("软件内部版本", 2),
    BOTH("两者均要适配", 3),
    NONE("两者均不适配", 4);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static AdaptiveSubject valOf(Integer val) {
        return Arrays.stream(AdaptiveSubject.values())
                .filter(adaptiveSubjectType -> adaptiveSubjectType.value == val)
                .findFirst()
                .orElse(null);
    }

}
