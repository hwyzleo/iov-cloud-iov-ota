package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 软件包制品可用性状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum SoftwarePackageState {

    ACTIVE("可用", 1),
    DEPRECATED("停用", 2),
    REVOKED("吊销", 3),
    RETIRED("退役", 4);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static SoftwarePackageState valOf(Integer val) {
        if (val == null) {
            return null;
        }
        return Arrays.stream(SoftwarePackageState.values())
                .filter(state -> state.value == val)
                .findFirst()
                .orElse(null);
    }

}
