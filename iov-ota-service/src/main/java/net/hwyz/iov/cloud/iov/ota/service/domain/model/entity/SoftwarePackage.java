package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Data;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.DeviceCode;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePackageId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SoftwarePn;

import java.io.Serializable;
import java.util.Date;

/**
 * 软件包实体
 */
@Data
@Builder
public class SoftwarePackage implements Serializable {
    private SoftwarePackageId id;
    private DeviceCode deviceCode;
    private SoftwarePn softwarePn;
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
