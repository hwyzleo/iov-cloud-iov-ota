package net.hwyz.iov.cloud.iov.ota.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 中央计算平台云端在线固件升级信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudFotaInfoCcp {

    /**
     * 基线代码
     */
    private String baselineCode;

    /**
     * 升级活动版本
     */
    private String activityVersion;

    /**
     * 活动发布时间
     */
    private Date activityReleaseTime;

    /**
     * 升级目的
     */
    private String upgradePurpose;

    /**
     * 升级功能项
     */
    private String upgradeFunction;

    /**
     * 活动说明
     */
    private String activityStatement;

    /**
     * 升级任务开始时间
     */
    private Date taskStartTime;

    /**
     * 升级任务结束时间
     */
    private Date taskEndTime;

    /**
     * 保持驻车(P档)
     */
    private Boolean keepInPark;

    /**
     * 不在充电
     */
    private Boolean notCharging;

    /**
     * 不对外供电
     */
    private Boolean noExternalPower;

    /**
     * 车窗、天窗、车门及尾门关闭
     */
    private Boolean allClosed;

    /**
     * 高压电量
     */
    private Integer hvSoc;

    /**
     * 低压电量
     */
    private Integer lvSoc;

    /**
     * 是否影响车辆操作
     */
    private Boolean impactVehicleOperation;

    /**
     * 刷写次数
     */
    private Integer flashCount;

    /**
     * 是否回滚
     */
    private Boolean rollback;

    /**
     * 升级模式
     */
    private Integer upgradeMode;

    /**
     * 升级计划时间
     */
    private Date scheduleTime;

    /**
     * 设备信息列表
     */
    private List<DeviceInfoCcp> deviceInfoList;

}
