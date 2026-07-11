package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.AdaptiveLevel;

import java.util.Date;

/**
 * 零件内部版本依赖值对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareBuildVersionDependencyVo {

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
    private AdaptiveLevel adaptiveLevel;

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
     * 软件零件是否支持OTA
     */
    private Boolean softwarePartOta;

    /**
     * 软件零件是否有解闭锁安全件
     */
    private Boolean softwarePartLockUnlockSecurityComponent;

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
     * 软件来源：BOM/OTA
     */
    private String softwareSource;

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
    private Date publishDate;

    /**
     * 创建时间
     */
    private Date createTime;

}
