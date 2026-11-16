package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 包当前下载进度（IOV-OTA-DSN-CR-026 §8.1 packages[].downloadProgress）。
 *
 * <p>只表达 VehicleTask + Package + downloadAttemptNo 的观测事实，不替代阶段终态，
 * 也不表示安装就绪。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class DownloadProgressProcessView {

    private Integer downloadAttemptNo;
    private String downloadState;
    private Long latestProgressSeq;
    private Long downloadedBytes;
    private Long totalBytes;
    private Long resumeOffset;
    /** 云端按字节计算的百分比 */
    private BigDecimal progressPercent;
    private String etag;
    private String errorCode;
    private String failureReason;
    /** 车端发生时间（未知为 null） */
    private Instant occurredAt;
    /** 云端受理时间 */
    private Instant receivedAt;
}
