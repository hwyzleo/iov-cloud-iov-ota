package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 下载进度受理命令（IOV-OTA-DSN-CR-026 §5.1、§7.1）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadProgressCmd {

    /** 进度ID（全局第一幂等键） */
    private String progressId;
    /** 车辆任务ID */
    private Long vehicleTaskId;
    private String packageId;
    private String packageRevision;
    /** 下载尝试序号（独立于安装 Execution attempt） */
    private Integer downloadAttemptNo;
    /** 进度序号（同一尝试内严格递增） */
    private Long progressSeq;
    /** 下载状态：STARTED/PROGRESS/PAUSED/RESUMED/RESET_OFFSET/FAILED/COMPLETED，未知值归一为 null */
    private String downloadState;
    private Long downloadedBytes;
    private Long totalBytes;
    private Long resumeOffset;
    /** 车端发生时间（occurred_at_ms=0 已归一为 null） */
    private Instant occurredAt;
    /** 事件摘要（小写十六进制） */
    private String eventDigest;
    private String errorCode;
    private String failureReason;
    private String etag;
    private String vin;
}
