package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;

import java.time.Instant;

/**
 * 创建软件内部版本命令
 */
@Data
public class CreateSoftwareBuildVersionCmd {
    private String deviceCode;
    private String softwarePn;
    private String softwareBuildVer;
    private String softwareReport;
    private String softwareDesc;
    private String softwareSource;
    private String adaptiveAssemblyPn;
    private String adaptiveSoftwarePn;
    private Instant releaseDate;
    private String createBy;
}
