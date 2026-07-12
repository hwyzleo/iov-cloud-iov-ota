package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;

/**
 * 创建软件内部版本命令
 */
@Data
public class CreateSoftwareBuildVersionCmd {
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String changeNote;
    private String softwareSource;
    private String createBy;
}
