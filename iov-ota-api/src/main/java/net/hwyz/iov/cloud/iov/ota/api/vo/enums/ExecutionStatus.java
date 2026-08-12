package net.hwyz.iov.cloud.iov.ota.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 安装执行状态枚举类（CR-012 §2.3）
 *
 * <p>Execution 是一次安装尝试的状态通道，与 VehicleTask 状态分离。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ExecutionStatus {

    PERMITTED("已许可"),
    INSTALLING("安装中"),
    PAUSED("已暂停"),
    ROLLING_BACK("回滚中"),
    SUCCEEDED("已成功"),
    FAILED("已失败"),
    ROLLED_BACK("已回滚"),
    CANCELED("已取消"),
    TIMED_OUT("已超时");

    public final String label;
    public final String value = name();

    public static ExecutionStatus valOf(String val) {
        if (val == null) {
            return null;
        }
        for (ExecutionStatus status : values()) {
            if (status.value.equals(val)) {
                return status;
            }
        }
        return null;
    }
}
