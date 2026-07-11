package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * UpgradePackage 领域实体
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Accessors(chain = true)
public class UpgradePackage {

    private Long id;
    private Long activityId;
    private Long softwareBuildVersionId;
    private String packageType;
    private String baseSoftwarePn;
    private String baseSoftwareVer;
    private String targetSoftwarePn;
    private String targetSoftwareVer;
    private String packageUrl;
    private Long packageSize;
    private String packageMd5;
    private String packageSha256;
    private String signRef;
    private String encryptRef;
    private String buildState;
    private String testState;
    private Boolean ota;
}
