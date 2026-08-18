package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 升级日志登记（CR-015 §3.3 upgradeLogs 子资源）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class UpgradeLogResult {

    private Long taskId;
    private String vinMasked;
    private String logUrl;
    private String uploadState;
    private Instant uploadTime;
}
