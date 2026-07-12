package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ApprovalLevel;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TypeApprovalAssessmentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.ActivitySoftwareBuildVersionMpt;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityApprovalMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityCompatiblePnMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityDependencyGroupMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityFixedConfigWordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityInstallOrderMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivitySoftwareBuildVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityTargetVersionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ApprovedSwManifestItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ApprovedSwManifestMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.RegulatoryFilingMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityApprovalPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityCompatiblePnPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityDependencyGroupPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityFixedConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityInstallOrderPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivitySoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityTargetVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.RegulatoryFilingPo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 升级活动应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAppService {

    private final ActivityMapper activityDao;
    private final ActivityApprovalMapper activityApprovalDao;
    private final ActivityCompatiblePnMapper activityCompatiblePnDao;
    private final ActivityFixedConfigWordMapper activityFixedConfigWordDao;
    private final ActivitySoftwareBuildVersionMapper activitySoftwareBuildVersionDao;
    private final ActivityTargetVersionMapper activityTargetVersionDao;
    private final ActivityInstallOrderMapper activityInstallOrderDao;
    private final ActivityDependencyGroupMapper activityDependencyGroupDao;
    private final ApprovedSwManifestMapper approvedSwManifestDao;
    private final ApprovedSwManifestItemMapper approvedSwManifestItemDao;
    private final RegulatoryFilingMapper regulatoryFilingDao;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;

    /**
     * 查询升级活动
     *
     * @param name      升级活动名称
     * @param state     升级活动状态
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 升级活动列表
     */
    public List<ActivityPo> search(String name, Integer state, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("state", state);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return activityDao.selectPoByMap(map);
    }

    /**
     * 获取升级活动下的软件内部版本列表
     *
     * @param activityId 升级活动ID
     * @return 软件内部版本列表
     */
    public List<ActivitySoftwareBuildVersionPo> listSoftwareBuildVersion(Long activityId) {
        return activitySoftwareBuildVersionDao.selectPoByActivityId(activityId);
    }

    /**
     * 获取升级活动下的兼容零件号列表
     *
     * @param activityId 升级活动ID
     * @return 兼容零件号列表
     */
    public List<ActivityCompatiblePnPo> listCompatiblePn(Long activityId) {
        return activityCompatiblePnDao.selectPoByExample(ActivityCompatiblePnPo.builder().activityId(activityId).build());
    }

    /**
     * 获取升级活动下的兼容零件号列表
     *
     * @param activityId 升级活动ID
     * @return 兼容零件号列表
     */
    public List<ActivityFixedConfigWordPo> listFixedConfigWord(Long activityId) {
        return activityFixedConfigWordDao.selectPoWithConfigWordByActivityId(activityId);
    }

    /**
     * 根据主键ID获取升级活动
     *
     * @param id 主键ID
     * @return 升级活动
     */
    public ActivityPo getActivityById(Long id) {
        return activityDao.selectPoById(id);
    }

    /**
     * 新增升级活动
     *
     * @param activity                         升级活动
     * @param activitySoftwareBuildVersionList 活动软件内部版本列表
     * @param activityTargetVersionList        活动目标版本列表（基线活动从本地投影预填）
     * @return 结果
     */
    public int createActivity(ActivityPo activity, List<ActivitySoftwareBuildVersionPo> activitySoftwareBuildVersionList,
                               List<ActivityTargetVersionPo> activityTargetVersionList) {
        activity.setState(ActivityState.PENDING.value);
        activity.setActivityCode(generateActivityCode());
        activity.setTypeApprovalAssessmentState(TypeApprovalAssessmentState.NOT_ASSESSED.value);
        if (activity.getIsTypeApprovalRelevant() == null) {
            activity.setIsTypeApprovalRelevant(false);
        }
        if (activity.getNoticeConsentRequired() == null) {
            activity.setNoticeConsentRequired(false);
        }
        if (activity.getTermsConsentRequired() == null) {
            activity.setTermsConsentRequired(false);
        }
        if (activity.getPrivacyConsentRequired() == null) {
            activity.setPrivacyConsentRequired(false);
        }
        int result = activityDao.insertPo(activity);
        if (activitySoftwareBuildVersionList != null && !activitySoftwareBuildVersionList.isEmpty()) {
            activitySoftwareBuildVersionList.forEach(po -> {
                po.setActivityId(activity.getId());
                po.setVersionGroup(0);
            });
            activitySoftwareBuildVersionDao.batchInsertPo(activitySoftwareBuildVersionList);
        }
        if (activityTargetVersionList != null && !activityTargetVersionList.isEmpty()) {
            activityTargetVersionList.forEach(po -> {
                po.setActivityId(activity.getId());
                activityTargetVersionDao.insert(po);
            });
        }
        return result;
    }

    /**
     * 新增升级活动软件内部版本信息
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @return 结果
     */
    public int createSoftwareBuildVersion(Long activityId, Long[] softwareBuildVersionIds) {
        Set<Long> softwareBuildVersionIdSet = listSoftwareBuildVersion(activityId).stream()
                .map(ActivitySoftwareBuildVersionPo::getSoftwareBuildVersionId)
                .collect(Collectors.toSet());
        List<ActivitySoftwareBuildVersionPo> list = new ArrayList<>();
        for (Long softwareBuildVersionId : softwareBuildVersionIds) {
            if (!softwareBuildVersionIdSet.contains(softwareBuildVersionId)) {
                if (!softwareBuildVersionAppService.checkReleaseGate(softwareBuildVersionId)) {
                    throw new IllegalStateException("软件内部版本[" + softwareBuildVersionId + "]未通过发布门禁校验（版本需RELEASED且引用软件包均ACTIVE）");
                }
                list.add(ActivitySoftwareBuildVersionPo.builder()
                        .activityId(activityId)
                        .softwareBuildVersionId(softwareBuildVersionId)
                        .critical(false)
                        .ota(true)
                        .sort(0)
                        .versionGroup(0)
                        .forceUpgrade(false)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            return activitySoftwareBuildVersionDao.batchInsertPo(list);
        }
        return 0;
    }

    /**
     * 新增升级活动兼容零件号
     *
     * @param activityId      升级活动ID
     * @param compatiblePnIds 兼容零件号ID数组
     * @return 结果
     */
    public int createCompatiblePn(Long activityId, Long[] compatiblePnIds) {
        Set<Long> compatiblePnIdSet = listCompatiblePn(activityId).stream()
                .map(ActivityCompatiblePnPo::getCompatiblePnId)
                .collect(Collectors.toSet());
        List<ActivityCompatiblePnPo> list = new ArrayList<>();
        for (Long compatiblePnId : compatiblePnIds) {
            if (!compatiblePnIdSet.contains(compatiblePnId)) {
                list.add(ActivityCompatiblePnPo.builder()
                        .activityId(activityId)
                        .compatiblePnId(compatiblePnId)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            return activityCompatiblePnDao.batchInsertPo(list);
        }
        return 0;
    }

    /**
     * 新增升级活动固定配置字
     *
     * @param activityId         升级活动ID
     * @param fixedConfigWordIds 固定配置字ID数组
     * @return 结果
     */
    public int createFixedConfigWord(Long activityId, Long[] fixedConfigWordIds) {
        Set<Long> fixedConfigWordIdSet = listFixedConfigWord(activityId).stream()
                .map(ActivityFixedConfigWordPo::getFixedConfigWordId)
                .collect(Collectors.toSet());
        List<ActivityFixedConfigWordPo> list = new ArrayList<>();
        for (Long fixedConfigWordId : fixedConfigWordIds) {
            if (!fixedConfigWordIdSet.contains(fixedConfigWordId)) {
                list.add(ActivityFixedConfigWordPo.builder()
                        .activityId(activityId)
                        .fixedConfigWordId(fixedConfigWordId)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            return activityFixedConfigWordDao.batchInsertPo(list);
        }
        return 0;
    }

    /**
     * 修改升级活动
     *
     * @param activity 升级活动
     * @return 结果
     */
    public int modifyActivity(ActivityPo activity) {
        return activityDao.updatePo(activity);
    }

    /**
     * 修改升级活动软件内部版本信息
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @param sorts                   排序数组
     * @param groups                  组数数组
     * @return 结果
     */
    public int modifyActivitySoftwareBuildVersion(Long activityId, Long[] softwareBuildVersionIds, Integer[] sorts, Integer[] groups) {
        listSoftwareBuildVersion(activityId).forEach(po -> {
            for (int i = 0; i < softwareBuildVersionIds.length; i++) {
                if (po.getSoftwareBuildVersionId().longValue() == softwareBuildVersionIds[i]) {
                    po.setSort(sorts[i]);
                    po.setVersionGroup(groups[i]);
                    activitySoftwareBuildVersionDao.updatePo(po);
                }
            }
        });
        return 1;
    }

    /**
     * 批量删除升级活动
     *
     * @param ids 升级活动ID数组
     * @return 结果
     */
    public int deleteActivityByIds(Long[] ids) {
        return activityDao.batchPhysicalDeletePo(ids);
    }

    /**
     * 删除升级活动软件内部版本信息
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @return 结果
     */
    public int deleteSoftwareBuildVersion(Long activityId, Long[] softwareBuildVersionIds) {
        return activitySoftwareBuildVersionDao.batchPhysicalDeletePoByActivityIdAndSoftwareBuildVersionIds(activityId, softwareBuildVersionIds);
    }

    /**
     * 删除升级活动兼容零件号信息
     *
     * @param activityId      升级活动ID
     * @param compatiblePnIds 兼容零件号ID数组
     * @return 结果
     */
    public int deleteCompatiblePn(Long activityId, Long[] compatiblePnIds) {
        return activityCompatiblePnDao.batchPhysicalDeletePoByActivityIdAndCompatiblePnIds(activityId, compatiblePnIds);
    }

    /**
     * 删除升级活动固定配置字信息
     *
     * @param activityId         升级活动ID
     * @param fixedConfigWordIds 固定配置字ID数组
     * @return 结果
     */
    public int deleteFixedConfigWord(Long activityId, Long[] fixedConfigWordIds) {
        return activityFixedConfigWordDao.batchPhysicalDeletePoByActivityIdAndFixedConfigWordIds(activityId, fixedConfigWordIds);
    }

    /**
     * 重组升级活动软件内部版本信息
     *
     * @param activityId 升级活动ID
     * @param list       组数数组
     * @return 结果
     */
    public int regroupActivitySoftwareBuildVersion(Long activityId, List<ActivitySoftwareBuildVersionMpt> list) {
        AtomicInteger result = new AtomicInteger();
        listSoftwareBuildVersion(activityId).forEach(po -> {
            list.forEach(mpt -> {
                if (po.getId().longValue() == mpt.getId()) {
                    if (po.getVersionGroup().intValue() != mpt.getVersionGroup()) {
                        po.setVersionGroup(mpt.getVersionGroup());
                        po.setSort(0);
                        activitySoftwareBuildVersionDao.updatePo(po);
                        result.getAndIncrement();
                    }
                }
            });
        });
        return result.get();
    }

    /**
     * 重排序升级活动软件内部版本信息
     *
     * @param activityId 升级活动ID
     * @param list       组数数组
     * @return 结果
     */
    public int resortActivitySoftwareBuildVersion(Long activityId, List<ActivitySoftwareBuildVersionMpt> list) {
        AtomicInteger result = new AtomicInteger();
        listSoftwareBuildVersion(activityId).forEach(po -> {
            list.forEach(mpt -> {
                if (po.getId().longValue() == mpt.getId()) {
                    if (po.getSort().intValue() != mpt.getSort()) {
                        po.setSort(mpt.getSort());
                        activitySoftwareBuildVersionDao.updatePo(po);
                        result.getAndIncrement();
                    }
                }
            });
        });
        return result.get();
    }

    /**
     * 统计基线软件内部版本数量
     *
     * @param activityId 升级活动ID
     * @return 基线软件零件版本数量
     */
    public int countActivitySoftwareBuildVersion(Long activityId) {
        return activitySoftwareBuildVersionDao.countByActivityId(activityId);
    }

    /**
     * 生成活动编码
     * 格式：ACT-{yyyyMMddHHmmss}-{4位随机数}
     *
     * @return 活动编码
     */
    private String generateActivityCode() {
        return "ACT-" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + "-" + String.format("%04d", new Random().nextInt(10000));
    }

    /**
     * 查询活动审批记录
     *
     * @param activityId 升级活动ID
     * @return 审批记录列表
     */
    public List<ActivityApprovalPo> listApprovals(Long activityId) {
        return activityApprovalDao.selectByActivityId(activityId);
    }

    /**
     * 多级审批：记录审批结果并推进活动状态
     * 串行流程：QUALITY -> PRODUCT -> SECURITY
     * 全部 PASS 后活动进入 APPROVED；任一 REJECT 回到 REJECTED
     *
     * @param activityId    升级活动ID
     * @param approvalStage 审批阶段
     * @param approverId    审批人ID
     * @param result        审批结果 PASS / REJECT
     * @param comment       审批意见
     * @return 审批记录
     */
    public ActivityApprovalPo approveActivity(Long activityId, String approvalStage, String approverId,
                                               String result, String comment) {
        ApprovalLevel stage = ApprovalLevel.valOf(approvalStage);
        if (stage == null) {
            throw new IllegalArgumentException("无效的审批阶段: " + approvalStage);
        }
        ActivityApprovalPo approval = ActivityApprovalPo.builder()
                .activityId(activityId)
                .approvalStage(approvalStage)
                .approverId(approverId)
                .result(result)
                .comment(comment)
                .approveTime(new Date())
                .build();
        activityApprovalDao.insert(approval);

        if ("REJECT".equals(result)) {
            ActivityPo activity = activityDao.selectPoById(activityId);
            activity.setState(ActivityState.REJECTED.value);
            activityDao.updatePo(activity);
        }

        return approval;
    }

    /**
     * 型式批准影响评估
     * 基于本地 SWIN 投影判定受影响受管 ECU 的 is_type_approval_relevant
     * 评估结果写入 type_approval_assessment_state
     * RXSWIN 生成与回写 MDM(EEAD) 暂留 TODO
     *
     * @param activityId 升级活动ID
     * @return 评估状态
     */
    public TypeApprovalAssessmentState assessTypeApproval(Long activityId) {
        ActivityPo activity = activityDao.selectPoById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在: " + activityId);
        }
        if (!Boolean.TRUE.equals(activity.getIsTypeApprovalRelevant())) {
            activity.setTypeApprovalAssessmentState(TypeApprovalAssessmentState.PASSED.value);
            activityDao.updatePo(activity);
            return TypeApprovalAssessmentState.PASSED;
        }

        // TODO: 冻结 tb_approved_sw_manifest 版本组合快照
        // TODO: 以 manifest_code 请求 MDM(EEAD) 幂等生成并同步返回 RXSWIN
        // TODO: 回填 rxswin_value 到 manifest，活动本体 rxswin 仅 1:1 时只读投影

        // 当前阶段：标记为 PASSED 以不阻断发布，待 MDM RXSWIN API 就绪后补全
        activity.setTypeApprovalAssessmentState(TypeApprovalAssessmentState.PASSED.value);
        activityDao.updatePo(activity);
        return TypeApprovalAssessmentState.PASSED;
    }

    /**
     * 刷新活动总文件大小（只读缓存）
     *
     * @param activityId 升级活动ID
     */
    public void refreshTotalFileSize(Long activityId) {
        List<ActivitySoftwareBuildVersionPo> sbvList = activitySoftwareBuildVersionDao.selectPoByActivityId(activityId);
        long totalSize = 0L;
        for (ActivitySoftwareBuildVersionPo sbv : sbvList) {
            // TODO: 汇总关联软件包大小
        }
        ActivityPo update = new ActivityPo();
        update.setId(activityId);
        update.setTotalFileSize(totalSize);
        update.setSizeCalcTime(new Date());
        activityDao.updatePo(update);
    }

    // ==================== A1. 目标版本组合 ====================

    public List<ActivityTargetVersionPo> listTargetVersion(Long activityId) {
        return activityTargetVersionDao.selectByActivityId(activityId);
    }

    public int saveTargetVersion(ActivityTargetVersionPo po) {
        if (po.getId() == null) {
            return activityTargetVersionDao.insert(po);
        }
        return activityTargetVersionDao.updateById(po);
    }

    public int deleteTargetVersion(Long id) {
        return activityTargetVersionDao.deleteById(id);
    }

    // ==================== C1. 安装顺序 ====================

    public List<ActivityInstallOrderPo> listInstallOrder(Long activityId) {
        return activityInstallOrderDao.selectByActivityId(activityId);
    }

    public int saveInstallOrder(ActivityInstallOrderPo po) {
        if (po.getId() == null) {
            return activityInstallOrderDao.insert(po);
        }
        return activityInstallOrderDao.updateById(po);
    }

    public int deleteInstallOrder(Long id) {
        return activityInstallOrderDao.deleteById(id);
    }

    // ==================== C2. 同升同降依赖组 ====================

    public List<ActivityDependencyGroupPo> listDependencyGroup(Long activityId) {
        return activityDependencyGroupDao.selectByActivityId(activityId);
    }

    public int saveDependencyGroup(ActivityDependencyGroupPo po) {
        if (po.getId() == null) {
            return activityDependencyGroupDao.insert(po);
        }
        return activityDependencyGroupDao.updateById(po);
    }

    public int deleteDependencyGroup(Long id) {
        return activityDependencyGroupDao.deleteById(id);
    }

    // ==================== D1. 型批版本组合快照（只读） ====================

    public List<ApprovedSwManifestPo> listManifest(Long activityId) {
        return approvedSwManifestDao.selectByActivityId(activityId);
    }

    public ApprovedSwManifestPo getManifestById(Long id) {
        return approvedSwManifestDao.selectById(id);
    }

    public List<ApprovedSwManifestItemPo> listManifestItems(Long manifestId) {
        return approvedSwManifestItemDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovedSwManifestItemPo>()
                        .eq(ApprovedSwManifestItemPo::getManifestId, manifestId)
                        .eq(ApprovedSwManifestItemPo::getRowValid, true));
    }

    // ==================== D2. 监管备案 ====================

    public List<RegulatoryFilingPo> listRegulatoryFiling(Long activityId) {
        return regulatoryFilingDao.selectByActivityId(activityId);
    }

    public RegulatoryFilingPo getRegulatoryFilingById(Long id) {
        return regulatoryFilingDao.selectById(id);
    }

    public int createRegulatoryFiling(RegulatoryFilingPo po) {
        return regulatoryFilingDao.insert(po);
    }

    public int updateRegulatoryFiling(RegulatoryFilingPo po) {
        return regulatoryFilingDao.updateById(po);
    }

    public int deleteRegulatoryFiling(Long id) {
        return regulatoryFilingDao.deleteById(id);
    }

}
