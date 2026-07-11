package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

/**
 * 升级包构建状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum UpgradePackageBuildState {

    PENDING("等待构建", "PENDING"),
    RUNNING("构建中", "RUNNING"),
    SUCCESS("构建成功", "SUCCESS"),
    FAIL("构建失败", "FAIL");

    public final String label;
    public final String value;

    public static UpgradePackageBuildState valOf(String val) {
        for (UpgradePackageBuildState state : values()) {
            if (state.value.equals(val)) {
                return state;
            }
        }
        return null;
    }
}
