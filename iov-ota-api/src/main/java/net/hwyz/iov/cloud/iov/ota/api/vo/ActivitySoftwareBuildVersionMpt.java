package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台升级活动下软件内部版本信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActivitySoftwareBuildVersionMpt extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 软件内部版本ID
     */
    private Long softwareBuildVersionId;

    /**
     * 是否关键版本
     */
    private Boolean critical;

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
     * 软件零件版本
     */
    private String softwarePartVer;

    /**
     * 软件内部版本
     */
    private String softwareBuildVer;

    /**
     * 软件来源：BOM/OTA
     */
    private String softwareSource;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 软件版本组
     */
    private Integer versionGroup;

    /**
     * 是否强制升级
     */
    private Boolean forceUpgrade;

    /**
     * 创建时间
     */
    private Date createTime;

}
