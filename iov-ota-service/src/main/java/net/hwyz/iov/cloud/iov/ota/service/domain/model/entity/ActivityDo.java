package net.hwyz.iov.cloud.iov.ota.service.domain.activity.model;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.iov.ota.api.contract.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityPo;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 升级活动领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@SuperBuilder
public class ActivityDo extends BaseDo<Long> implements DomainObj<ActivityDo> {

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动版本
     */
    private String version;

    /**
     * 升级须知文章ID
     */
    private Long upgradeNoticeArticleId;

    /**
     * 活动条款文章ID
     */
    private Long activityTermArticleId;

    /**
     * 隐私协议文章ID
     */
    private Long privacyAgreementArticleId;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 活动发布时间
     */
    private Date releaseTime;

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
    private String statement;

    /**
     * 活动状态
     */
    private ActivityState activityState;

    /**
     * 是否基线活动
     */
    private Boolean baseline;

    /**
     * 基线代码
     */
    private String baselineCode;

    /**
     * 备注
     */
    private String description;

    /**
     * 分组软件内部版本信息Map
     */
    private Map<Integer, List<ActivitySoftwareBuildVersionVo>> groupSoftwareBuildVersionMap;

    /**
     * 固定配置字信息列表
     */
    private List<ConfigWordVo> fixedConfigWordList;

    /**
     * 兼容零件号Map
     */
    private Map<String, Set<String>> compatiblePnMap;

    /**
     * 初始化
     */
    public void init() {
        stateInit();
    }

    /**
     * 加载信息
     *
     * @param groupSoftwareBuildVersionMap 分组软件内部版本信息Map
     * @param fixedConfigWordList          固定配置字信息列表
     * @param compatiblePnMap              兼容零件号Map
     */
    public void load(Map<Integer, List<ActivitySoftwareBuildVersionVo>> groupSoftwareBuildVersionMap, List<ConfigWordVo> fixedConfigWordList,
                     Map<String, Set<String>> compatiblePnMap) {
        this.groupSoftwareBuildVersionMap = groupSoftwareBuildVersionMap;
        this.fixedConfigWordList = fixedConfigWordList;
        this.compatiblePnMap = compatiblePnMap;
        stateLoad();
    }

    /**
     * 修改信息
     *
     * @param activityPo 升级活动
     */
    public void edit(ActivityPo activityPo) {
        if (StrUtil.isNotBlank(activityPo.getName()) && !activityPo.getName().equals(this.name)) {
            this.name = activityPo.getName();
            stateChange();
        }
        if (StrUtil.isNotBlank(activityPo.getVersion()) && !activityPo.getVersion().equals(this.version)) {
            this.version = activityPo.getVersion();
            stateChange();
        }
        if (activityPo.getStartTime() != null && !activityPo.getStartTime().equals(this.startTime)) {
            this.startTime = activityPo.getStartTime();
            stateChange();
        }
        if (activityPo.getEndTime() != null && !activityPo.getEndTime().equals(this.endTime)) {
            this.endTime = activityPo.getEndTime();
            stateChange();
        }
    }

    /**
     * 提交活动
     *
     * @param activityPo 升级活动
     */
    public int submit(ActivityPo activityPo) {
        if (this.activityState == ActivityState.PENDING) {
            edit(activityPo);
            this.activityState = ActivityState.SUBMITTED;
            stateChange();
            return 1;
        }
        return 0;
    }

    /**
     * 审核活动
     *
     * @param audit  审核结果
     * @param reason 拒绝原因
     * @return 1: 成功，0: 失败
     */
    public int audit(Boolean audit, String reason) {
        if (this.activityState == ActivityState.SUBMITTED) {
            if (audit) {
                this.activityState = ActivityState.APPROVED;
            } else {
                this.activityState = ActivityState.REJECTED;
                this.description = reason;
            }
            stateChange();
            return 1;
        }
        return 0;
    }

    /**
     * 发布活动
     *
     * @return 1: 成功，0: 失败
     */
    public int release() {
        if (this.activityState == ActivityState.APPROVED) {
            this.releaseTime = new Date();
            this.activityState = ActivityState.RELEASED;
            stateChange();
            return 1;
        }
        return 0;
    }

    /**
     * 取消活动
     *
     * @return 1: 成功，0: 失败
     */
    public int cancel() {
        if (this.activityState == ActivityState.RELEASED) {
            this.activityState = ActivityState.CANCELLED;
            stateChange();
            return 1;
        }
        return 0;
    }

    /**
     * 检查活动前置条件
     *
     * @param vehicle 车辆
     * @return true: 满足条件，false: 不满足条件
     */
    public boolean checkPreconditions(VehicleDo vehicle) {
        if (!checkCriticalDevices(vehicle)) {
            return false;
        }
        return true;
    }

    /**
     * 检查车辆关键设备是否满足升级条件
     *
     * @param vehicle 车辆
     * @return true: 满足条件，false: 不满足条件
     */
    private boolean checkCriticalDevices(VehicleDo vehicle) {
        if (this.baseline) {
            for (List<ActivitySoftwareBuildVersionVo> list : groupSoftwareBuildVersionMap.values()) {
                for (ActivitySoftwareBuildVersionVo entity : list) {
                    if (entity.getCritical() && !vehicle.getDeviceMap().containsKey(entity.getSoftwareBuildVersion().getDeviceCode())) {
                        logger.warn("车辆[{}]关键设备[{}]不满足升级条件", vehicle.getId(), entity.getSoftwareBuildVersion().getDeviceCode());
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
