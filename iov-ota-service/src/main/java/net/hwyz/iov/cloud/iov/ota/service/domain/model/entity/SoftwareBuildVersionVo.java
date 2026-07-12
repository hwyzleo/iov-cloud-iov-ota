package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 零件内部版本信息值对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareBuildVersionVo {

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
     * 技术变更说明
     */
    private String changeNote;

    /**
     * 软件来源：BOM/OTA
     */
    private String softwareSource;

    /**
     * 发布工作流状态
     */
    private String buildState;

    /**
     * 适配的总成硬件零件号
     */
    private String adaptiveHardwarePn;

    /**
     * 发布时间
     */
    private Date releaseTime;

    /**
     * 软件包数量
     */
    private Integer softwarePackageCount;

    /**
     * 依赖数量
     */
    private Integer dependencyCount;

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
