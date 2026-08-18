package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 升级日志上传摘要（CR-015 §3.3 logSummary）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class UpgradeLogProcessView {

    private String logUrl;
    private String uploadState;
    private Instant uploadTime;
}
