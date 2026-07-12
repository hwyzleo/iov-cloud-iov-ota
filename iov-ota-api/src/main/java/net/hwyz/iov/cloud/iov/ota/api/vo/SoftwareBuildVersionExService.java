package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 对外服务软件内部版本信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareBuildVersionExService {

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
     * 发布工作流状态：1-草稿,2-测试中,3-已发布,4-停用,5-退役
     */
    private Integer buildState;

    /**
     * 发布时间
     */
    private Date releaseTime;

    /**
     * 软件包列表
     */
    private List<SoftwarePackageExService> softwarePackageList;

    /**
     * 软件内部版本依赖列表
     */
    private List<SoftwareBuildVersionDependencyExService> dependencyList;

    /**
     * 创建时间
     */
    private Date createTime;

}
