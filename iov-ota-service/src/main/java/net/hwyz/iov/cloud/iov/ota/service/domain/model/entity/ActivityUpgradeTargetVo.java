package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 活动升级对象值对象
 *
 * @author hwyz_leo
 * @since 2026-07-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityUpgradeTargetVo {

    /**
     * 主键
     */
    private Long id;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 来源类型：0 手动，1 基线
     */
    private Integer sourceType;

    /**
     * 基线代码（source_type=1时必填）
     */
    private String baselineCode;

    /**
     * 车载节点代码
     */
    private String vehicleNodeCode;

    /**
     * 软件零件号
     */
    private String partCode;

    /**
     * 软件内部版本ID
     */
    private Long softwareBuildVersionId;

    /**
     * 是否关键版本
     */
    private Boolean critical;

    /**
     * 是否支持OTA
     */
    private Boolean ota;

    /**
     * 安装顺序号
     */
    private Integer installSeq;

    /**
     * 并行组号
     */
    private Integer parallelGroup;

    /**
     * 活动内分组号
     */
    private Integer groupNo;

    /**
     * 是否强制升级
     */
    private Boolean forceUpgrade;

    /**
     * 软件内部版本（聚合信息）
     */
    private SoftwareBuildVersionVo softwareBuildVersion;

    /**
     * 软件包列表（聚合信息）
     */
    private List<SoftwarePackageVo> softwarePackageList;

    /**
     * 软件内部版本依赖列表（聚合信息）
     */
    private List<SoftwareBuildVersionDependencyVo> softwareBuildVersionDependencyList;

    /**
     * 配置字列表（聚合信息）
     */
    private List<ConfigWordVo> configWordList;

    /**
     * 创建时间
     */
    private Date createTime;
}
