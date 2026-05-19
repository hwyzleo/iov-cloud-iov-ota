package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

/**
 * 软件包结果DTO
 */
@Data
@Builder
public class SoftwarePackageDto {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String packageCode;
    private String packageName;
    private String packageType;
    private String packageUrl;
    private String packageMd5;
    private Long packageSize;
    private String packageDesc;
}
