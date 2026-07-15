package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 升级目的枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum UpgradePurpose {

    BUGFIX("缺陷修复", 1),
    FEATURE("功能新增", 2),
    SECURITY("安全补丁", 3),
    COMPLIANCE("合规整改", 4),
    OTHER("其他", 9);

    /**
     * 名称
     */
    public final String label;
    /**
     * 值
     */
    public final int value;

    public static UpgradePurpose valOf(Integer val) {
        if (val == null) {
            return null;
        }
        return Arrays.stream(UpgradePurpose.values())
                .filter(upgradePurpose -> upgradePurpose.value == val)
                .findFirst()
                .orElse(null);
    }

}
