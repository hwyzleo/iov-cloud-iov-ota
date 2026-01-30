package net.hwyz.iov.cloud.ota.pota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 开放平台BOM软件内部版本依赖
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomSoftwareBuildVersionDependencyOapi {

    /**
     * 主键
     */
    private Long id;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 软件零件号
     */
    private String softwarePn;

    /**
     * 软件零件名称
     */
    private String softwarePartName;

    /**
     * 软件零件版本
     */
    private String softwarePartVer;

    /**
     * 软件内部版本
     */
    private String softwareBuildVer;

    /**
     * 依赖描述
     */
    private String description;

    /**
     * 更新时间
     */
    private Date modifyTime;

}
