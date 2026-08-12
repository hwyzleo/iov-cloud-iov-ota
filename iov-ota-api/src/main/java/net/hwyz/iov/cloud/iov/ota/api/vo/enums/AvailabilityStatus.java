package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务可见性状态枚举类（CR-012 §5.2、§7）
 *
 * <p>availabilityStatus 是任务可见/可操作判定结果，与 VehicleTaskStatus 不同义。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum AvailabilityStatus {

    NONE("无可匹配任务"),
    NOT_RELEASED("未发布"),
    AVAILABLE("可用"),
    BLOCKED("已阻断"),
    PAUSED("已暂停"),
    CANCELED("已取消"),
    SUPERSEDED("已取代");

    public final String label;
    public final String value = name();

    public static AvailabilityStatus valOf(String val) {
        if (val == null) {
            return null;
        }
        for (AvailabilityStatus status : values()) {
            if (status.value.equals(val)) {
                return status;
            }
        }
        return null;
    }
}
