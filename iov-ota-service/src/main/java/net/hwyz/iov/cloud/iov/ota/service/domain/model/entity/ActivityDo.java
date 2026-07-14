package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TypeApprovalAssessmentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradePurpose;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
     * 活动编码（系统生成·全局唯一·不可变）
     */
    private String activityCode;

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
     * 发行说明文章ID
     */
    private Long releaseNoteArticleId;

    /**
     * 活动开始时间（外层约束窗口起）
     */
    private Date startTime;

    /**
     * 活动结束时间（外层约束窗口止）
     */
    private Date endTime;

    /**
     * 活动发布时间
     */
    private Date releaseTime;

    /**
     * 升级目的
     */
    private UpgradePurpose upgradePurpose;

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
     * 总文件大小（字节·只读缓存）
     */
    private Long totalFileSize;

    /**
     * 文件大小缓存计算时间
     */
    private Date sizeCalcTime;

    /**
     * 是否基线活动
     */
    private Boolean baseline;

    /**
     * 基线代码
     */
    private String baselineCode;

    /**
     * 是否型批相关
     */
    private Boolean isTypeApprovalRelevant;

    /**
     * 型批影响评估状态
     */
    private TypeApprovalAssessmentState typeApprovalAssessmentState;

    /**
     * 须知是否需显式同意
     */
    private Boolean noticeConsentRequired;

    /**
     * 条款是否需显式同意
     */
    private Boolean termsConsentRequired;

    /**
     * 隐私是否需显式同意
     */
    private Boolean privacyConsentRequired;

    /**
     * RXSWIN值（只读·manifest回填·仅1:1场景）
     */
    private String rxswin;

    /**
     * 备注
     */
    private String description;

    /**
     * 分组软件内部版本信息Map
     */
    private Map<Integer, List<ActivityUpgradeTargetVo>> groupUpgradeTargetMap;

    /**
     * 固定配置字信息列表
     */
    private List<ConfigWordVo> fixedConfigWordList;

    /**
     * 初始化
     */
    public void init() {
        stateInit();
    }

    /**
     * 加载信息
     *
     * @param groupUpgradeTargetMap 分组升级对象信息Map
     * @param fixedConfigWordList   固定配置字信息列表
     */
    public void load(Map<Integer, List<ActivityUpgradeTargetVo>> groupUpgradeTargetMap, List<ConfigWordVo> fixedConfigWordList) {
        this.groupUpgradeTargetMap = groupUpgradeTargetMap;
        this.fixedConfigWordList = fixedConfigWordList;
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
        if (activityPo.getStartTime() != null && !activityPo.getStartTime().equals(this.startTime)) {
            this.startTime = activityPo.getStartTime();
            stateChange();
        }
        if (activityPo.getEndTime() != null && !activityPo.getEndTime().equals(this.endTime)) {
            this.endTime = activityPo.getEndTime();
            stateChange();
        }
        if (activityPo.getUpgradePurpose() != null) {
            UpgradePurpose purpose = UpgradePurpose.valOf(activityPo.getUpgradePurpose());
            if (purpose != null && purpose != this.upgradePurpose) {
                this.upgradePurpose = purpose;
                stateChange();
            }
        }
        if (StrUtil.isNotBlank(activityPo.getStatement()) && !activityPo.getStatement().equals(this.statement)) {
            this.statement = activityPo.getStatement();
            stateChange();
        }
        if (activityPo.getReleaseNoteArticleId() != null && !activityPo.getReleaseNoteArticleId().equals(this.releaseNoteArticleId)) {
            this.releaseNoteArticleId = activityPo.getReleaseNoteArticleId();
            stateChange();
        }
        if (activityPo.getNoticeConsentRequired() != null && !activityPo.getNoticeConsentRequired().equals(this.noticeConsentRequired)) {
            this.noticeConsentRequired = activityPo.getNoticeConsentRequired();
            stateChange();
        }
        if (activityPo.getTermsConsentRequired() != null && !activityPo.getTermsConsentRequired().equals(this.termsConsentRequired)) {
            this.termsConsentRequired = activityPo.getTermsConsentRequired();
            stateChange();
        }
        if (activityPo.getPrivacyConsentRequired() != null && !activityPo.getPrivacyConsentRequired().equals(this.privacyConsentRequired)) {
            this.privacyConsentRequired = activityPo.getPrivacyConsentRequired();
            stateChange();
        }
        if (activityPo.getIsTypeApprovalRelevant() != null && !activityPo.getIsTypeApprovalRelevant().equals(this.isTypeApprovalRelevant)) {
            this.isTypeApprovalRelevant = activityPo.getIsTypeApprovalRelevant();
            stateChange();
        }
    }

    /**
     * 提交活动
     * 提交后 is_baseline / baseline_code 不可修改
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
     * 审核活动（多级审批的一环，外部编排串行调用）
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
     * 型批相关且评估状态非 PASSED 时阻断发布
     *
     * @return 1: 成功，0: 失败
     */
    public int release() {
        if (this.activityState == ActivityState.APPROVED) {
            if (Boolean.TRUE.equals(this.isTypeApprovalRelevant)
                    && this.typeApprovalAssessmentState != TypeApprovalAssessmentState.PASSED) {
                log.warn("活动[{}]型批相关但评估状态非PASSED，阻断发布", this.getId());
                return 0;
            }
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
            for (List<ActivityUpgradeTargetVo> list : groupUpgradeTargetMap.values()) {
                for (ActivityUpgradeTargetVo entity : list) {
                    if (entity.getCritical() && !vehicle.getDeviceMap().containsKey(entity.getVehicleNodeCode())) {
                        log.warn("车辆[{}]关键设备[{}]不满足升级条件", vehicle.getId(), entity.getVehicleNodeCode());
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
