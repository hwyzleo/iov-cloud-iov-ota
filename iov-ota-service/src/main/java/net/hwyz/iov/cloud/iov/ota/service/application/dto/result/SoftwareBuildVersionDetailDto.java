package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 软件内部版本详情DTO
 */
@Data
@Builder
public class SoftwareBuildVersionDetailDto {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String changeNote;
    private String softwareSource;
    private String buildState;
    private Instant releaseTime;
    private List<SoftwarePackageDto> packages;
    private List<SoftwareBuildVersionDependencyDto> dependencies;
}
