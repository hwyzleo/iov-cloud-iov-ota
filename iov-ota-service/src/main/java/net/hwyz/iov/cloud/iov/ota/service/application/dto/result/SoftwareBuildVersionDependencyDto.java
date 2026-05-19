package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

/**
 * 软件内部版本依赖结果DTO
 */
@Data
@Builder
public class SoftwareBuildVersionDependencyDto {
    private Long id;
    private Long dependencySoftwareBuildVersionId;
    private String dependencyDeviceCode;
    private String dependencySoftwarePn;
    private String dependencySoftwareBuildVer;
    private Integer adaptiveLevel;
}
