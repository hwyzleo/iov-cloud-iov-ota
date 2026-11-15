package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Builder;
import lombok.Data;

/**
 * 车辆任务重置命令（IOV-OTA-DSN-CR-025 §4.29.5）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VehicleTaskResetCmd {

    /** 待重置的车辆任务ID（必须是当前车辆任务） */
    private Long vehicleTaskId;

    /** 重置原因（必填，≤512） */
    private String reason;

    /** 是否强制重置（存在活动 Execution 时必须显式 true） */
    private boolean force;

    /** 可选乐观锁版本（提供时校验并发覆盖） */
    private Long rowVersion;

    /** 操作人（审计写入） */
    private String operator;
}
