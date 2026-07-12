package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 软件内部版本结果DTO
 */
@Data
@Builder
public class SoftwareBuildVersionDto {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String changeNote;
    private String softwareSource;
    private String buildState;
    private Instant releaseTime;
    private Integer softwarePackageCount;
    private Integer dependencyCount;
    private Integer testReportCount;
    private Integer adaptationCount;
    private Integer adaptiveLevel;
}
