package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.json.JSONObject;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.iov.ota.api.vo.CloudFotaInfoCcp;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.*;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeMode;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeModeArg;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.util.FotaHelper;

import java.util.*;

/**
 * 升级任务车辆领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@SuperBuilder
public class TaskVehicleDo extends BaseDo<Long> implements DomainObj<TaskVehicleDo> {

    /**
     * 基线代码
     */
    private String baselineCode;

    /**
     * 升级活动版本
     */
    private String activityVersion;

    /**
     * 升级活动发布时间
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
     * 升级模式
     */
    private UpgradeMode upgradeMode;

    /**
     * 升级模式参数
     */
    private JSONObject upgradeModeArg;

    /**
     * 策略列表
     */
    private Map<TaskStrategyType, String> strategyMap;

    /**
     * 软件内部版本列表
     */
    private List<TaskVehicleSoftwareBuildVersionVo> softwareBuildVersionList;

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
     * 车辆任务状态
     */
    private TaskVehicleState taskState;

    /**
     * 初始化
     */
    public void init() {
        stateInit();
    }

    /**
     * 加载基础信息
     *
     * @param activity 升级活动
     * @param task     升级任务
     */
    public void loadBaseInfo(ActivityDo activity, Task task) {
        this.baselineCode = activity.getBaselineCode();
        this.activityVersion = activity.getVersion();
        this.activityReleaseTime = activity.getReleaseTime();
        this.upgradePurpose = activity.getUpgradePurpose() != null ? activity.getUpgradePurpose().label : null;
        this.upgradeFunction = activity.getUpgradeFunction();
        this.activityStatement = activity.getStatement();
        this.taskStartTime = task.getStartTimeDate();
        this.taskEndTime = task.getEndTimeDate();
        this.upgradeMode = task.getUpgradeMode();
        this.upgradeModeArg = task.getUpgradeModeArgJson();
    }

    public void loadStrategy(Task task) {
        this.strategyMap = new HashMap<>();
        if (task.getTaskStrategyList() != null) {
            task.getTaskStrategyList().forEach(taskStrategy -> this.strategyMap.put(taskStrategy.getStrategyType(), taskStrategy.getStrategyExpression()));
        }
    }

    public void loadSoftwareBuildVersion(ActivityDo activity, Task task, VehicleDo vehicle, FotaHelper fotaHelper) {
        this.softwareBuildVersionList = new ArrayList<>();
        Map<Integer, List<ActivitySoftwareBuildVersionVo>> groupSoftwareBuildVersionMap = activity.getGroupSoftwareBuildVersionMap();
        TaskVehicleSoftwareBuildVersionVo taskVehicleSoftwareBuildVersion;
        for (Integer group : groupSoftwareBuildVersionMap.keySet()) {
            List<ActivitySoftwareBuildVersionVo> groupSoftwareBuildVersionList = groupSoftwareBuildVersionMap.get(group);
            if (groupAdaptationMatch(groupSoftwareBuildVersionList, vehicle, activity, task)) {
                for (ActivitySoftwareBuildVersionVo activitySoftwareBuildVersion : groupSoftwareBuildVersionList) {
                    if (!activitySoftwareBuildVersion.getOta()) {
                        log.info("车辆[{}]设备[{}]版本[{}]不支持OTA升级，忽略", activitySoftwareBuildVersion.getSoftwareBuildVersion().getDeviceCode(),
                                activitySoftwareBuildVersion.getSoftwareBuildVersion().getSoftwareBuildVer(), vehicle.getId());
                        continue;
                    }
                    List<ConfigWordVo> softwareConfigWordList = activitySoftwareBuildVersion.getConfigWordList();
                    List<ConfigWordVo> fixedConfigWordList = activity.getFixedConfigWordList();
                    taskVehicleSoftwareBuildVersion = new TaskVehicleSoftwareBuildVersionVo();
                    taskVehicleSoftwareBuildVersion.setGroup(group);
                    taskVehicleSoftwareBuildVersion.setForceUpgrade(activitySoftwareBuildVersion.getForceUpgrade());
                    taskVehicleSoftwareBuildVersion.setSoftwareBuildVersion(activitySoftwareBuildVersion.getSoftwareBuildVersion());
                    taskVehicleSoftwareBuildVersion.setSoftwarePackageList(activitySoftwareBuildVersion.getSoftwarePackageList());
                    taskVehicleSoftwareBuildVersion.setSoftwareBuildVersionDependencyList(activitySoftwareBuildVersion.getSoftwareBuildVersionDependencyList());
                    taskVehicleSoftwareBuildVersion.setConfigWordList(softwareConfigWordList);
                    DeviceInfoVo vehicleDeviceInfo = vehicle.getDeviceMap().get(activitySoftwareBuildVersion.getSoftwareBuildVersion().getDeviceCode());
                    if (Boolean.parseBoolean(this.strategyMap.get(TaskStrategyType.ROLLBACK))) {
                        List<SoftwarePackageVo> rollbackSoftwarePackage = fotaHelper.getSoftwareBuildVersionPackages(vehicleDeviceInfo.getDeviceCode(),
                                vehicleDeviceInfo.getSoftwarePn() + vehicleDeviceInfo.getSoftwarePartVer(),
                                vehicleDeviceInfo.getSoftwareBuildVer());
                        taskVehicleSoftwareBuildVersion.setRollbackSoftwarePackageList(rollbackSoftwarePackage);
                    }
                    if (!softwareConfigWordList.isEmpty()) {
                        taskVehicleSoftwareBuildVersion.setOriginConfigWord(vehicleDeviceInfo.getConfigWord());
                        taskVehicleSoftwareBuildVersion.setTargetConfigWord(fotaHelper.configWordToStr(vehicleDeviceInfo.getConfigWord(), softwareConfigWordList));
                    }
                    if (!fixedConfigWordList.isEmpty()) {
                        taskVehicleSoftwareBuildVersion.setOriginConfigWord(vehicleDeviceInfo.getConfigWord());
                        taskVehicleSoftwareBuildVersion.setTargetConfigWord(fotaHelper.configWordToStr(vehicleDeviceInfo.getConfigWord(), fixedConfigWordList));
                    }
                    this.softwareBuildVersionList.add(taskVehicleSoftwareBuildVersion);
                }
            }
        }
        stateChange();
    }

    /**
     * 加载文章
     *
     * @param activity 升级活动
     */
    public void loadArticle(ActivityDo activity) {
        this.upgradeNoticeArticleId = activity.getUpgradeNoticeArticleId();
        this.activityTermArticleId = activity.getActivityTermArticleId();
        this.privacyAgreementArticleId = activity.getPrivacyAgreementArticleId();
    }

    /**
     * 转换为云端升级信息
     *
     * @return 云端升级信息
     */
    public CloudFotaInfoCcp toCloudFotaInfoCcp() {
        CloudFotaInfoCcp cloudFotaInfoCcp = new CloudFotaInfoCcp();
        cloudFotaInfoCcp.setBaselineCode(this.baselineCode);
        cloudFotaInfoCcp.setActivityVersion(this.activityVersion);
        cloudFotaInfoCcp.setActivityReleaseTime(this.activityReleaseTime);
        cloudFotaInfoCcp.setUpgradePurpose(this.upgradePurpose);
        cloudFotaInfoCcp.setUpgradeFunction(this.upgradeFunction);
        cloudFotaInfoCcp.setActivityStatement(this.activityStatement);
        cloudFotaInfoCcp.setTaskStartTime(this.taskStartTime);
        cloudFotaInfoCcp.setTaskEndTime(this.taskEndTime);
        if (this.strategyMap.containsKey(TaskStrategyType.KEEP_IN_PARK)) {
            cloudFotaInfoCcp.setKeepInPark(Boolean.valueOf(this.strategyMap.get(TaskStrategyType.KEEP_IN_PARK)));
        }
        if (this.strategyMap.containsKey(TaskStrategyType.NOT_CHARGING)) {
            cloudFotaInfoCcp.setNotCharging(Boolean.valueOf(this.strategyMap.get(TaskStrategyType.NOT_CHARGING)));
        }
        if (this.strategyMap.containsKey(TaskStrategyType.NO_EXTERNAL_POWER)) {
            cloudFotaInfoCcp.setNoExternalPower(Boolean.valueOf(this.strategyMap.get(TaskStrategyType.NO_EXTERNAL_POWER)));
        }
        if (this.strategyMap.containsKey(TaskStrategyType.ALL_CLOSED)) {
            cloudFotaInfoCcp.setAllClosed(Boolean.valueOf(this.strategyMap.get(TaskStrategyType.ALL_CLOSED)));
        }
        if (this.strategyMap.containsKey(TaskStrategyType.HV_SOC)) {
            cloudFotaInfoCcp.setHvSoc(Integer.parseInt(this.strategyMap.get(TaskStrategyType.HV_SOC)));
        }
        if (this.strategyMap.containsKey(TaskStrategyType.LV_SOC)) {
            cloudFotaInfoCcp.setLvSoc(Integer.parseInt(this.strategyMap.get(TaskStrategyType.LV_SOC)));
        }
        if (this.strategyMap.containsKey(TaskStrategyType.IMPACT_VEHICLE_OPERATION)) {
            cloudFotaInfoCcp.setImpactVehicleOperation(Boolean.valueOf(this.strategyMap.get(TaskStrategyType.IMPACT_VEHICLE_OPERATION)));
        }
        if (this.strategyMap.containsKey(TaskStrategyType.FLASH_COUNT)) {
            cloudFotaInfoCcp.setFlashCount(Integer.parseInt(this.strategyMap.get(TaskStrategyType.FLASH_COUNT)));
        }
        if (this.strategyMap.containsKey(TaskStrategyType.ROLLBACK)) {
            cloudFotaInfoCcp.setRollback(Boolean.valueOf(this.strategyMap.get(TaskStrategyType.ROLLBACK)));
        }
        cloudFotaInfoCcp.setUpgradeMode(this.upgradeMode.getValue());
        if (this.upgradeModeArg.containsKey(UpgradeModeArg.SCHEDULED_TIME.name())) {
            cloudFotaInfoCcp.setScheduleTime(this.upgradeModeArg.getDate(UpgradeModeArg.SCHEDULED_TIME.name()));
        }
        return cloudFotaInfoCcp;
    }

    /**
     * 更新任务车辆状态
     *
     * @param taskState 任务车辆状态
     */
    public void updateState(Integer taskState) {
        TaskVehicleState taskVehicleState = TaskVehicleState.valOf(taskState);
        if (taskVehicleState != null) {
            this.taskState = taskVehicleState;
            stateChange();
        } else {
            log.warn("任务车辆状态错误：{}", taskState);
        }
    }

    /**
     * 软件组适配比对
     * 同组内软件有任何一个不适配，不影响其他软件适配，直到适配结束
     * 非基线OTA软件适配，同组内软件有任何一个不适配，不进行升级
     *
     * @param groupSoftwareBuildVersionList 组软件内部版本列表
     * @param vehicle                       车辆
     * @param activity                      升级活动
     * @param task                          升级任务
     * @return true: 适配成功，false: 适配失败
     */
    private boolean groupAdaptationMatch(List<ActivitySoftwareBuildVersionVo> groupSoftwareBuildVersionList, VehicleDo vehicle,
                                         ActivityDo activity, Task task) {
        for (int i = groupSoftwareBuildVersionList.size() - 1; i > 0; i--) {
            ActivitySoftwareBuildVersionVo activitySoftwareBuildVersion = groupSoftwareBuildVersionList.get(i);
            SoftwareBuildVersionVo softwareBuildVersion = activitySoftwareBuildVersion.getSoftwareBuildVersion();
            DeviceInfoVo deviceInfo = vehicle.getDeviceMap().get(softwareBuildVersion.getDeviceCode());
            boolean adaptiveResult = false;
            if (deviceInfo == null) {
                log.warn("车辆[{}]待升级设备[{}]没有上报，跳过", vehicle.getId(), softwareBuildVersion.getDeviceCode());
            } else {
                adaptiveResult = deviceAdaptationMatch(deviceInfo, activitySoftwareBuildVersion, activity, task, vehicle);
            }
            if (!adaptiveResult) {
                if (!activity.getBaseline()) {
                    return false;
                }
                groupSoftwareBuildVersionList.remove(i);
            }
        }
        return true;
    }

    /**
     * 设备适配比对
     *
     * @param deviceInfo                   设备信息
     * @param activitySoftwareBuildVersion 活动软件内部版本
     * @param activity                     升级活动
     * @param task                         升级任务
     * @param vehicle                      车辆
     * @return true: 适配成功，false: 适配失败
     */
    private boolean deviceAdaptationMatch(DeviceInfoVo deviceInfo, ActivitySoftwareBuildVersionVo activitySoftwareBuildVersion,
                                          ActivityDo activity, Task task, VehicleDo vehicle) {
        TaskRestrictionVo comparisonCriteriaVo = task.getTaskRestrictionMap().get(TaskRestrictionType.COMPARISON_CRITERIA);
        boolean comparisonCriteria = Boolean.parseBoolean(comparisonCriteriaVo.getRestrictionExpression());
        SoftwareBuildVersionVo softwareBuildVersion = activitySoftwareBuildVersion.getSoftwareBuildVersion();
        Map<String, Set<String>> compatiblePnMap = activity.getCompatiblePnMap();
        boolean softwarePnMatch = softwareBuildVersion.getSoftwarePn().equals(deviceInfo.getSoftwarePn());
        if (!softwarePnMatch && comparisonCriteria) {
            softwarePnMatch = compatiblePnMap.get(deviceInfo.getDeviceCode()).contains(softwareBuildVersion.getSoftwarePn());
        }
        if (!softwarePnMatch) {
            log.info("车辆[{}]设备[{}]软件零件号[{}]与软件内部版本软件零件号[{}]不匹配，忽略", vehicle.getId(),
                    softwareBuildVersion.getDeviceCode(), deviceInfo.getSoftwarePn(), softwareBuildVersion.getSoftwarePn());
            return false;
        }
        TaskRestrictionVo adaptationSubjectVo = task.getTaskRestrictionMap().get(TaskRestrictionType.ADAPTATION_SUBJECT);
        AdaptiveSubject adaptationSubject = AdaptiveSubject.valOf(Integer.parseInt(adaptationSubjectVo.getRestrictionExpression()));
        for (SoftwarePackageVo softwarePackage : activitySoftwareBuildVersion.getSoftwarePackageList()) {
            if (adaptationSubject == AdaptiveSubject.NONE) {
                if (softwarePackage.getPackageType() == SoftwarePackageType.DELTA) {
                    // 即使适配主体无需比对，差分包仍然需要适配基础版本
                    boolean deltaPackageMatch = versionMatch(deviceInfo.getSoftwareBuildVer(), softwarePackage.getBaseSoftwareVer(), softwarePackage.getPackageAdaptiveLevel());
                    if (deltaPackageMatch) {
                        softwarePackage.setMatch(true);
                    } else {
                        log.warn("车辆[{}]设备[{}]软件包[{}]与软件内部版本差分软件包[{}]不匹配，忽略", vehicle.getId(),
                                softwareBuildVersion.getDeviceCode(), softwarePackage.getPackageName(), softwarePackage.getBaseSoftwarePn());
                    }
                    continue;
                }
                if (!isSoftwarePnLatest(softwareBuildVersion, deviceInfo) || !isSoftwareBuildVerLatest(softwareBuildVersion, deviceInfo)) {
                    softwarePackage.setMatch(true);
                    continue;
                } else {
                    // 当前设备已是最新版
                    return false;
                }
            }
            String adaptiveHardwarePn = softwareBuildVersion.getAdaptiveHardwarePn();
            if (adaptiveHardwarePn != null && !adaptiveHardwarePn.isEmpty()
                    && !adaptiveHardwarePn.contains(deviceInfo.getHardwarePn()) && !adaptiveHardwarePn.contains(deviceInfo.getPartNo())) {
                log.warn("车辆[{}]设备[{}]硬件零件号[{}:{}]与软件内部版本硬件零件号[{}]不匹配", vehicle.getId(), deviceInfo.getDeviceCode(),
                        deviceInfo.getHardwarePn(), deviceInfo.getPartNo(), adaptiveHardwarePn);
                return false;
            }
            if (activitySoftwareBuildVersion.getForceUpgrade()) {
                log.info("车辆[{}]设备[{}]软件包[{}]强制升级，跳过校验", vehicle.getId(), softwareBuildVersion.getDeviceCode(), softwarePackage.getPackageName());
                softwarePackage.setMatch(true);
                continue;
            }
            if (adaptationSubject == AdaptiveSubject.BOTH) {
                if (isSoftwarePnLatest(softwareBuildVersion, deviceInfo) && isSoftwareBuildVerLatest(softwareBuildVersion, deviceInfo)) {
                    log.info("车辆[{}]设备[{}]软件零件号[{}]内部版本[{}]已是最新版本，忽略", vehicle.getId(), deviceInfo.getDeviceCode(),
                            deviceInfo.getSoftwarePn(), deviceInfo.getSoftwareBuildVer());
                    return false;
                }
            }
            if (adaptationSubject == AdaptiveSubject.SOFTWARE_PN) {
                if (isSoftwarePnLatest(softwareBuildVersion, deviceInfo)) {
                    log.info("车辆[{}]设备[{}]软件零件号[{}]已是最新版，忽略", vehicle.getId(), deviceInfo.getDeviceCode(), deviceInfo.getSoftwarePn());
                    return false;
                }
            }
            if (adaptationSubject == AdaptiveSubject.SOFTWARE_BUILD_VERSION) {
                if (isSoftwareBuildVerLatest(softwareBuildVersion, deviceInfo)) {
                    log.info("车辆[{}]设备[{}]内部版本[{}]已是最新版，忽略", vehicle.getId(), deviceInfo.getDeviceCode(), deviceInfo.getSoftwareBuildVer());
                    return false;
                }
            }
            if (adaptationSubject == AdaptiveSubject.BOTH || adaptationSubject == AdaptiveSubject.SOFTWARE_PN) {
                if (!versionMatch(deviceInfo.getSoftwarePn() + deviceInfo.getSoftwarePartVer(),
                        softwarePackage.getSoftwarePn() + softwarePackage.getSoftwarePartVer(), softwarePackage.getPackageAdaptiveLevel())) {
                    log.warn("车辆[{}]设备[{}]软件零件版本[{}:{}]与软件内部版本软件零件版本[{}:{}]不匹配，忽略", vehicle.getId(),
                            softwareBuildVersion.getDeviceCode(), deviceInfo.getSoftwarePn(), deviceInfo.getSoftwarePartVer(),
                            softwarePackage.getSoftwarePn(), softwarePackage.getSoftwarePartVer());
                    continue;
                }
            }
            if (adaptationSubject == AdaptiveSubject.BOTH || adaptationSubject == AdaptiveSubject.SOFTWARE_BUILD_VERSION) {
                if (!versionMatch(deviceInfo.getSoftwareBuildVer(), softwarePackage.getBaseSoftwareVer(), softwarePackage.getPackageAdaptiveLevel())) {
                    log.warn("车辆[{}]设备[{}]软件内部版本[{}:{}]与软件内部版本软件内部版本[{}:{}]不匹配，忽略", vehicle.getId(),
                            softwareBuildVersion.getDeviceCode(), deviceInfo.getSoftwarePn(), deviceInfo.getSoftwareBuildVer(),
                            softwarePackage.getSoftwarePn(), softwarePackage.getBaseSoftwareVer());
                    continue;
                }
            }
            if (!activity.getBaseline()) {
                // 非基线需要进行依赖项校验
                if (!dependencyMatch(activitySoftwareBuildVersion.getSoftwareBuildVersionDependencyList(), vehicle,
                        comparisonCriteria, compatiblePnMap, adaptationSubject)) {
                    log.warn("车辆[{}]设备[{}]软件包[{}]依赖项不匹配，忽略", vehicle.getId(), softwareBuildVersion.getDeviceCode(), softwarePackage.getPackageName());
                    continue;
                }
            }
            softwarePackage.setMatch(true);
        }
        return true;
    }

    /**
     * 版本适配
     *
     * @param originVersion   原版本
     * @param targetVersion   目标版本
     * @param adaptationLevel 适配级别
     * @return true: 适配成功，false: 适配失败
     */
    private boolean versionMatch(String originVersion, String targetVersion, AdaptiveLevel adaptationLevel) {
        int compareResult = VersionComparator.INSTANCE.compare(originVersion, targetVersion);
        return switch (adaptationLevel) {
            case LE -> compareResult <= 0;
            case GE -> compareResult >= 0;
            case EQ -> compareResult == 0;
            default -> false;
        };
    }

    /**
     * 软件包依赖项适配
     *
     * @param softwareBuildVersionDependencyList 软件内部版本依赖项列表
     * @param vehicle                            车辆
     * @param comparisonCriteria                 比对基准是否兼容
     * @param compatiblePnMap                    兼容的零件号
     * @param adaptationSubject                  适配主体
     * @return true: 适配成功，false: 适配失败
     */
    private boolean dependencyMatch(List<SoftwareBuildVersionDependencyVo> softwareBuildVersionDependencyList, VehicleDo vehicle,
                                    boolean comparisonCriteria, Map<String, Set<String>> compatiblePnMap, AdaptiveSubject adaptationSubject) {
        if (softwareBuildVersionDependencyList != null) {
            for (SoftwareBuildVersionDependencyVo dependency : softwareBuildVersionDependencyList) {
                DeviceInfoVo deviceInfo = vehicle.getDeviceMap().get(dependency.getDeviceCode());
                if (deviceInfo == null) {
                    log.warn("车辆[{}]软件内部版本[{}]依赖设备[{}]不存在", vehicle.getId(), dependency.getSoftwareBuildVersionId(),
                            dependency.getDeviceCode());
                    return false;
                }
                boolean softwarePnMatch = dependency.getSoftwarePn().equals(deviceInfo.getSoftwarePn());
                if (!softwarePnMatch && comparisonCriteria) {
                    softwarePnMatch = compatiblePnMap.get(deviceInfo.getDeviceCode()).contains(dependency.getSoftwarePn());
                }
                if (!softwarePnMatch) {
                    log.info("车辆[{}]软件内部版本[{}]依赖设备[{}]软件零件号[{}]与软件内部版本软件零件号[{}]不匹配，忽略", vehicle.getId(),
                            dependency.getSoftwareBuildVersionId(), dependency.getDeviceCode(), deviceInfo.getSoftwarePn(), dependency.getSoftwarePn());
                    return false;
                }
                if (adaptationSubject == AdaptiveSubject.NONE) {
                    continue;
                }
                if (adaptationSubject == AdaptiveSubject.SOFTWARE_PN || adaptationSubject == AdaptiveSubject.BOTH) {
                    if (!versionMatch(deviceInfo.getSoftwarePn() + deviceInfo.getSoftwarePartVer(),
                            dependency.getSoftwarePn() + dependency.getSoftwarePartVer(), dependency.getAdaptiveLevel())) {
                        log.warn("车辆[{}]设备[{}]软件零件号[{}:{}]与软件内部版本软件零件号[{}:{}]不匹配，忽略", vehicle.getId(),
                                dependency.getDeviceCode(), deviceInfo.getSoftwarePn(), deviceInfo.getSoftwarePartVer(),
                                dependency.getSoftwarePn(), dependency.getSoftwarePartVer());
                        return false;
                    }
                }
                if (adaptationSubject == AdaptiveSubject.SOFTWARE_BUILD_VERSION || adaptationSubject == AdaptiveSubject.BOTH) {
                    if (!versionMatch(deviceInfo.getSoftwareBuildVer(), dependency.getSoftwareBuildVer(), dependency.getAdaptiveLevel())) {
                        log.warn("车辆[{}]设备[{}]软件内部版本[{}]与软件内部版本软件内部版本[{}]不匹配，忽略", vehicle.getId(),
                                dependency.getDeviceCode(), deviceInfo.getSoftwareBuildVer(), dependency.getSoftwareBuildVer());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 软件零件号是否最新
     *
     * @param softwareBuildVersion 软件内部版本
     * @param deviceInfo           设备信息
     * @return true: 是，false: 否
     */
    private boolean isSoftwarePnLatest(SoftwareBuildVersionVo softwareBuildVersion, DeviceInfoVo deviceInfo) {
        return softwareBuildVersion.getSoftwarePn().equals(deviceInfo.getSoftwarePn()) &&
                softwareBuildVersion.getSoftwarePartVer().equals(deviceInfo.getSoftwarePartVer());
    }

    /**
     * 软件内部版本是否最新
     *
     * @param softwareBuildVersion 软件内部版本
     * @param deviceInfo           设备信息
     * @return true: 是，false: 否
     */
    private boolean isSoftwareBuildVerLatest(SoftwareBuildVersionVo softwareBuildVersion, DeviceInfoVo deviceInfo) {
        return softwareBuildVersion.getSoftwareBuildVer().equals(deviceInfo.getSoftwareBuildVer());
    }

}
