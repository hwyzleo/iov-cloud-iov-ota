package net.hwyz.iov.cloud.iov.ota.api.vo;

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
     * 技术变更说明
     */
    private String changeNote;

    /**
     * 软件来源
     */
    private String softwareSource;

    /**
     * 发布时间
     */
    private Date releaseTime;

    /**
     * 创建时间
     */
    private Date createTime;

}
