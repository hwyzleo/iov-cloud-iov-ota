package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

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
     * 发布工作流状态：DRAFT-草稿,TESTING-测试中,RELEASED-已发布,DEPRECATED-停用,RETIRED-退役
     */
    private String buildState;

    /**
     * 发布时间
     */
    private Date releaseTime;

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
     * 测试报告数量
     */
    private Integer testReportCount;

    /**
     * 适配矩阵数量
     */
    private Integer adaptationCount;

    /**
     * 适配级别
     */
    private Integer adaptiveLevel;
}
