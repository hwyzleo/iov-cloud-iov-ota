package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 升级任务车辆软件内部版本信息值对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskVehicleSoftwareBuildVersionVo {

    /**
     * 分组
     */
    private Integer group;

    /**
     * 是否强制升级
     */
    private Boolean forceUpgrade;

    /**
     * 是否关键版本
     */
    private Boolean critical;

    /**
     * 软件内部版本
     */
    private SoftwareBuildVersionVo softwareBuildVersion;

    /**
     * 软件包列表
     */
    private List<SoftwarePackageVo> softwarePackageList;

    /**
     * 回滚软件包列表
     */
    private List<SoftwarePackageVo> rollbackSoftwarePackageList;

    /**
     * 软件内部版本依赖列表
     */
    private List<SoftwareBuildVersionDependencyVo> softwareBuildVersionDependencyList;

    /**
     * 配置字列表
     */
    private List<ConfigWordVo> configWordList;

    /**
     * 升级前配置字
     */
    private String originConfigWord;

    /**
     * 升级后配置字
     */
    private String targetConfigWord;

    /**
     * 创建时间
     */
    private Date createTime;

}
