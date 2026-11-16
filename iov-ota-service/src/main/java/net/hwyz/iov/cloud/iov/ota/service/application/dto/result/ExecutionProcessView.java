package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 安装尝试（Execution）过程（CR-015 §3.3 executions）
 * <p>含 attemptNo、状态、连续水位、终态与缺失区间；不返回下载凭证明文。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class ExecutionProcessView {

    /** 执行行 DB 主键（用于 /executions/{id}/events 子资源） */
    private Long id;

    /** 执行业务键 */
    private String executionId;

    private Integer attemptNo;
    private String status;
    private Long taskRevision;
    private String installPlanVersion;
    private Long acceptedSequenceNo;
    private Long finalSequenceNo;

    /** 缺失序列区间，如 "3-5,7"（无缺失为空串） */
    private String missingSequenceRanges;

    /** 是否当前活动执行 */
    private Boolean active;

    private String offlinePolicy;
    private String timeoutPolicy;
    private Instant validUntil;

    /** 车端实际安装开始时间（未知为 null，CR-026） */
    private Instant startTime;

    /** 车端实际安装结束时间（未知为 null，CR-026） */
    private Instant endTime;

    /** 开始时间对应的云端受理时间（CR-026） */
    private Instant startReceivedTime;

    /** 结束时间对应的云端受理时间（CR-026） */
    private Instant endReceivedTime;

    /** 过程耗时（毫秒）；起止任一未知或负耗时均为 null（CR-026） */
    private Long durationMs;
}
