package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.Data;

import java.time.Instant;

/**
 * 软件内部版本查询条件
 */
@Data
public class SoftwareBuildVersionQuery {
    private String deviceCode;
    private String softwarePn;
    private String baselineCode;
    private Instant beginTime;
    private Instant endTime;
}
