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
    private String eventType;
    private String eventDigest;
    private String disposition;
    private Instant receivedTime;
}
