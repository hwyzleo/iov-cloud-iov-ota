package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;

/**
 * 快照状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum ManifestStatus {

    FROZEN("已冻结", "FROZEN");

    public final String label;
    public final String value;

    public static ManifestStatus valOf(String val) {
        for (ManifestStatus status : values()) {
            if (status.value.equals(val)) {
                return status;
            }
        }
        return null;
    }
}
