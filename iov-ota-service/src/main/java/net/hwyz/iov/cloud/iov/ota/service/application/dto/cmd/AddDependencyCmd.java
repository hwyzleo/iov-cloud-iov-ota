package net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd;

import lombok.Data;

/**
 * 添加依赖命令
 */
@Data
public class AddDependencyCmd {
    private Long softwareBuildVersionId;
    private Long dependencySoftwareBuildVersionId;
    private Integer adaptiveLevel;
    private String createBy;
}
