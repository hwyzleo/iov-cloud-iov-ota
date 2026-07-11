package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

/**
 * 审批级别枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum ApprovalLevel {

    QUALITY("质量审批", "QUALITY"),
    PRODUCT("产品审批", "PRODUCT"),
    SECURITY("安全审批", "SECURITY");

    public final String label;
    public final String value;

    public static ApprovalLevel valOf(String val) {
        for (ApprovalLevel level : values()) {
            if (level.value.equals(val)) {
                return level;
            }
        }
        return null;
    }
}
