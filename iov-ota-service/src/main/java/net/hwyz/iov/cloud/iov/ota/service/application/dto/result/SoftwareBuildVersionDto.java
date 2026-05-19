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
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private String adaptiveAssemblyPn;
    private String adaptiveSoftwarePn;
    private Instant releaseDate;
    private Integer softwarePackageCount;
    private Integer dependencyCount;
    private Integer adaptiveLevel;
}
