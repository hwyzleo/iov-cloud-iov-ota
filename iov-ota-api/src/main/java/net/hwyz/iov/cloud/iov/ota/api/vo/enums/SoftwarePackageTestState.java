package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

/**
 * 软件包测试状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum SoftwarePackageTestState {

    UNTESTED("未测试", "UNTESTED"),
    TESTING("测试中", "TESTING"),
    PASSED("测试通过", "PASSED"),
    FAILED("测试不通过", "FAILED");

    public final String label;
    public final String value;

    public static SoftwarePackageTestState valOf(String val) {
        for (SoftwarePackageTestState state : values()) {
            if (state.value.equals(val)) {
                return state;
            }
        }
        return null;
    }
}
