package net.hwyz.iov.cloud.ota.pota.api.contract.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 软件包类型枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum SoftwarePackageType {

    FULL("全量", 1),
    DELTA("差分", 2);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static SoftwarePackageType valOf(Integer val) {
        return Arrays.stream(SoftwarePackageType.values())
                .filter(softwarePackageType -> softwarePackageType.value == val)
                .findFirst()
                .orElse(null);
    }

}
