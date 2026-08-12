package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 恢复查询结果（CR-012 §5.8、US-083）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryResult {

    /** 对账处置：CONSISTENT/CLOUD_ONLY/VEHICLE_ONLY/REVISION_CONFLICT/MANUAL_RECOVERY_REQUIRED */
    private String disposition;

    /** 车辆任务ID */
    private Long vehicleTaskId;

    /** 车辆任务状态 */
    private String vehicleTaskStatus;

    /** 执行ID */
    private Long executionId;

    /** 执行状态 */
    private String executionStatus;

    /** 尝试序号 */
    private Integer attemptNo;

    /** 当前连续水位 */
    private Long acceptedSequenceNo;

    /** 最终序号 */
    private Long finalSequenceNo;

    /** 缺失序号范围 */
    private List<long[]> missingSequenceRanges;

    /** 待处理控制的最新 revision */
    private Integer pendingControlRevision;

    /** 待处理控制的动作 */
    private String pendingControlAction;

    /** 恢复动作建议 */
    private String recoveryAction;

    /** 许可有效期 */
    private Instant validUntil;
}
