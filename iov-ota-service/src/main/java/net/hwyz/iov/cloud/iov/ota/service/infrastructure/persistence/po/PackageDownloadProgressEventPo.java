package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 包下载进度不可变历史 PO（IOV-OTA-DSN-CR-026 §6.2）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_package_download_progress_event")
public class PackageDownloadProgressEventPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("progress_id")
    private String progressId;

    @TableField("vehicle_task_id")
    private Long vehicleTaskId;

    @TableField("package_id")
    private String packageId;

    @TableField("package_revision")
    private String packageRevision;

    @TableField("download_attempt_no")
    private Integer downloadAttemptNo;

    @TableField("progress_seq")
    private Long progressSeq;

    @TableField("download_state")
    private String downloadState;

    @TableField("downloaded_bytes")
    private Long downloadedBytes;

    @TableField("total_bytes")
    private Long totalBytes;

    @TableField("resume_offset")
    private Long resumeOffset;

    @TableField("progress_percent")
    private BigDecimal progressPercent;

    @TableField("etag")
    private String etag;

    @TableField("error_code")
    private String errorCode;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("event_digest")
    private String eventDigest;

    @TableField("disposition")
    private String disposition;

    @TableField("source_occurred_at")
    private Date sourceOccurredAt;

    @TableField("received_at")
    private Date receivedAt;
}
