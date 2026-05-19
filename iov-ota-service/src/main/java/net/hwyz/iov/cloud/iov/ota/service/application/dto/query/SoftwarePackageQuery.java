package net.hwyz.iov.cloud.iov.ota.service.application.dto.query;

import lombok.Data;

/**
 * 软件包查询条件
 */
@Data
public class SoftwarePackageQuery {
    private String deviceCode;
    private String softwarePn;
    private String packageCode;
    private String packageName;
    private Long softwareBuildVersionId;
}
