package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

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
    private String packageSha256;
    private String packageSignature;
    private String signAlgo;
    private String signerCertId;
    private Long packageSize;
    private String packageDesc;
    private String packageSource;
    private String baseSoftwarePn;
    private String baseSoftwareVer;
    private Integer packageAdaptiveLevel;
    private String adaptiveAssemblyPn;
    private Date releaseDate;
    private Integer estimatedInstallTime;
    private Boolean ota;
}
