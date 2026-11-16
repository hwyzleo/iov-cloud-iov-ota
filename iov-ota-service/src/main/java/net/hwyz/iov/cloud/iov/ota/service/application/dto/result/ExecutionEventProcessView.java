package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 安装执行事件（CR-015 §3.3 executions/{id}/events 子资源）
 * <p>事件以 eventId 与 (executionId, sequenceNo) 双重唯一键；只返回摘要，不返回 payload bytes。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class ExecutionEventProcessView {

    private String eventId;
    private Long executionId;
    private Long sequenceNo;
    /** 阶段（INSTALL/REBOOT/POST_CHECK/ROLLBACK，与 eventType 同值） */
    private String stage;
    private String eventType;
    private String eventDigest;
    private String disposition;
    private Instant receivedTime;

    /** 车端发生时间（未知为 null，CR-026） */
    private Instant occurredAt;

    /** 事件携带百分比（0..100，CR-026） */
    private Integer progressPercent;

    private String ecuId;
    private String softwareTargetCode;
    private String slot;
    private String errorCode;
    private String failureReason;
}
