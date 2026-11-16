package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 包阶段结果历史（IOV-OTA-DSN-CR-026 §8.2）。
 *
 * <p>返回每个包各阶段的**历史行**，不再只取每包最新一条；数据库分页由 Mapper SQL 完成。
 * 不返回预签名地址、凭证或事件原始报文。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class PackageStageHistoryResult {

    private Long id;
    private Long vehicleTaskId;
    private String packageId;
    private String stage;
    private String resultStatus;
    private String packageRevision;
    private String etag;
    private String digest;
    private String signatureResult;
    private String decryptResult;
    private String failReason;

    /** 下载尝试序号（历史数据未知为 null，不伪造） */
    private Integer downloadAttemptNo;

    /** 车端发生时间（未知为 null） */
    private Instant occurredAt;

    /** 云端受理时间（历史数据未知为 null） */
    private Instant receivedAt;
}
