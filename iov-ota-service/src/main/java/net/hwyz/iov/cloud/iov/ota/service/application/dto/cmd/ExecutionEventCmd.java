package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 安装事件命令（CR-012 §5.6、US-080）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEventCmd {

    /** 执行ID */
    private Long executionId;

    /** 事件ID（幂等键） */
    private String eventId;

    /** 事件序号 */
    private Long sequenceNo;

    /** 事件类型（阶段：INSTALL/REBOOT/POST_CHECK/ROLLBACK） */
    private String eventType;

    /** 事件状态字符串（如 INSTALL_STARTED/SUCCEEDED/FAILED，CR-026 §6.3） */
    private String eventStatus;

    /** 车端发生时间（occurred_at_ms=0 已归一为 null，不伪造） */
    private Instant occurredAt;

    /** 事件携带百分比 0..100（可选） */
    private Integer progressPercent;

    /** ECU标识（白名单结构化列） */
    private String ecuId;

    /** 软件目标标识（MULTI_TARGET） */
    private String softwareTargetCode;

    /** 槽位 */
    private String slot;

    /** 错误码 */
    private String errorCode;

    /** 错误详情（截断后落库） */
    private String failureReason;

    /** 事件摘要（防冲突） */
    private String eventDigest;

    /** 事件负载（JSON） */
    private String eventPayload;

    /** 车架号 */
    private String vin;
}
