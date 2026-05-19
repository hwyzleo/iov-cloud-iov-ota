package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;

/**
 * 添加软件包命令
 */
@Data
public class AddPackageCmd {
    private Long softwareBuildVersionId;
    private String deviceCode;
    private String softwarePn;
    private String packageCode;
    private String packageName;
    private String packageType;
    private String packageUrl;
    private String packageMd5;
    private Long packageSize;
    private String packageDesc;
    private String createBy;
}
