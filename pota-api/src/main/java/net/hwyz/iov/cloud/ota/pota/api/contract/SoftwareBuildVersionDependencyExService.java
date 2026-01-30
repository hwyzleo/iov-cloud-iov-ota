package net.hwyz.iov.cloud.ota.pota.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 对外服务软件内部版本信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareBuildVersionDependencyExService {

    /**
     * 主键
     */
    private Long id;

    /**
     * 软件内部版本ID
     */
    private Long softwareBuildVersionId;

    /**
     * 依赖软件内部版本ID
     */
    private Long dependencySoftwareBuildVersionId;

    /**
     * 适配级别：1-版本及以下，2-版本及以上，3-与版本一致
     */
    private Integer adaptiveLevel;

    /**
     * 排序
     */
    private Integer sort;

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
     * 软件来源
     */
    private String softwareSource;

    /**
     * 适配的总成零件号
     */
    private String adaptiveAssemblyPn;

    /**
     * 适配的软件零件号
     */
    private String adaptiveSoftwarePn;

    /**
     * 发布日期
     */
    private Date releaseDate;

    /**
     * 创建时间
     */
    private Date createTime;

}
