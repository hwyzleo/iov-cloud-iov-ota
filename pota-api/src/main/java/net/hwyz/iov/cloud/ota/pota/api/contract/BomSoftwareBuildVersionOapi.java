package net.hwyz.iov.cloud.ota.pota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 开放平台BOM软件内部版本信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomSoftwareBuildVersionOapi {

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
     * 软件零件版本
     */
    private String softwarePartVer;

    /**
     * 软件内部版本
     */
    private String softwareBuildVer;

    /**
     * 软件测试报告
     */
    private String softwareReport;

    /**
     * 软件说明
     */
    private String softwareDesc;

    /**
     * 软件来源：1-BOM，2-OTA
     */
    private Integer softwareSource;

    /**
     * 适配的总成硬件零件号
     */
    private String adaptiveHardwarePn;

    /**
     * 适配的总成软件零件号
     */
    private String adaptiveSoftwarePn;

    /**
     * 发布日期
     */
    private Date releaseDate;

    /**
     * 软件包列表
     */
    private List<BomSoftwarePackageOapi> softwarePackageList;

    /**
     * 软件内部版本依赖列表
     */
    private List<BomSoftwareBuildVersionDependencyOapi> softwareBuildVersionDependencyList;

    /**
     * 配置字依赖列表
     */
    private List<BomConfigWordDependencyOapi> configWordDependencyList;

    /**
     * 适配级别
     * 作为依赖时使用
     */
    private Integer adaptiveLevel;

    /**
     * 创建时间
     */
    private Date createTime;

}
