package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;

/**
 * 修改软件内部版本命令
 */
@Data
public class ModifySoftwareBuildVersionCmd {
    private Long id;
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String changeNote;
    private String softwareSource;
    private String modifyBy;
}
