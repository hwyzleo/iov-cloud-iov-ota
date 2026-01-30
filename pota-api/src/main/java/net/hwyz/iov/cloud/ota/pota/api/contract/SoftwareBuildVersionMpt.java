package net.hwyz.iov.cloud.ota.pota.api.contract;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.web.domain.BaseRequest;

import java.util.Date;

/**
 * 管理后台软件内部版本信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SoftwareBuildVersionMpt extends BaseRequest {

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
     */
    private Integer adaptiveLevel;
}
