package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下载进度受理结果（IOV-OTA-DSN-CR-026 §5.1）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadProgressResult {

    /** 是否受理（LATE_OBSERVATION 也为 true，但不推进当前快照） */
    private boolean accepted;
    /** 处置：CURRENT/LATE_OBSERVATION/DUPLICATE/CONFLICT/REJECTED */
    private String disposition;
    /** 当前快照序号（无快照为 0；DUPLICATE 复用首次结果） */
    private Long latestAcceptedProgressSeq;
    /** 拒绝原因（稳定常量） */
    private String denyReason;
}
