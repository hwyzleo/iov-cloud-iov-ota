package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 云端控制与回执摘要（CR-015 §3.3 controls）
 * <p>展示最新 controlRevision 与最新回执摘要；PAUSE/ABORT/ROLLBACK 为云端意图，车端保留安全裁决权。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class ControlProcessSummary {

    private Integer controlCount;
    private Integer latestControlRevision;
    private String latestAction;
    private String latestScope;
    private String latestApplyMode;
    private String latestAckStatus;
    private Integer latestAckSequenceNo;
    private Instant latestAckTime;
}
