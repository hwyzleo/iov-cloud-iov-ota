package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 车辆任务状态枚举类（CR-012 §2.2）
 *
 * <p>VehicleTask 是单 VIN 长期任务的状态通道，管理任务快照、授权、下载准备与车辆级状态。
 * 该状态与接口 availabilityStatus 不是同义词；availability 是任务可见/可操作判定结果。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum VehicleTaskStatus {

    CREATED("已创建"),
    VISIBLE("已可见"),
    CONSENT_PENDING("待授权"),
    DOWNLOAD_PENDING("待下载"),
    READY_TO_INSTALL("就绪可安装"),
    EXECUTING("执行中"),
    RETRY_PENDING("待重试"),
    ROLLBACK_PENDING("待回滚"),
    SUCCEEDED("已成功"),
    ROLLED_BACK("已回滚"),
    PAUSED("已暂停"),
    CANCELED("已取消"),
    SUPERSEDED("已取代");

    public final String label;
    public final String value = name();

    public static VehicleTaskStatus valOf(String val) {
        if (val == null) {
            return null;
        }
        for (VehicleTaskStatus status : values()) {
            if (status.value.equals(val)) {
                return status;
            }
        }
        return null;
    }
}
