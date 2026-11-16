package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 下载进度历史事件（IOV-OTA-DSN-CR-026 §8.2 downloadEvents 子资源）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class DownloadProgressEventResult {

    private Long id;
    private String progressId;
    private Integer downloadAttemptNo;
    private Long progressSeq;
    private String downloadState;
    private Long downloadedBytes;
    private Long totalBytes;
    private Long resumeOffset;
    private BigDecimal progressPercent;
    private String etag;
    private String errorCode;
    private String failureReason;
    private String disposition;
    private Instant occurredAt;
    private Instant receivedAt;
}
