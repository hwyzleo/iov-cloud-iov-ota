package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 执行对账处置枚举类（CR-012 §5.8、§7）
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ExecutionDisposition {

    CONSISTENT("一致"),
    CLOUD_ONLY("仅云端"),
    VEHICLE_ONLY("仅车端"),
    REVISION_CONFLICT("版本冲突"),
    MANUAL_RECOVERY_REQUIRED("需人工恢复");

    public final String label;
    public final String value = name();

    public static ExecutionDisposition valOf(String val) {
        if (val == null) {
            return null;
        }
        for (ExecutionDisposition disposition : values()) {
            if (disposition.value.equals(val)) {
                return disposition;
            }
        }
        return null;
    }
}
