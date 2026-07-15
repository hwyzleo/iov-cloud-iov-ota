package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mdm.api.service.MdmRxswinRegistryService;
import net.hwyz.iov.cloud.edd.mdm.api.vo.request.RxswinRegisterRequest;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.RxswinRegistryResponse;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ApprovalLevel;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TypeApprovalAssessmentState;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TypeApprovalBaseline;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TypeApprovalBaselineRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityApprovalMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityFixedConfigWordMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityUpgradeTargetMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ActivityGroupPolicyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ApprovedSwManifestItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ApprovedSwManifestMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.RegulatoryFilingMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwareBuildVersionPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwarePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SwinManagedSystemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityApprovalPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityFixedConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityGroupPolicyPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityUpgradeTargetPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.RegulatoryFilingPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SwinManagedSystemPo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    private final ActivityFixedConfigWordMapper activityFixedConfigWordDao;
    private final ActivityUpgradeTargetMapper activityUpgradeTargetDao;
    private final ActivityGroupPolicyMapper activityGroupPolicyDao;
    private final ApprovedSwManifestMapper approvedSwManifestDao;
    private final ApprovedSwManifestItemMapper approvedSwManifestItemDao;
    private final RegulatoryFilingMapper regulatoryFilingDao;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;
    private final SoftwareBuildVersionPackageMapper softwareBuildVersionPackageDao;
    private final SoftwarePackageMapper softwarePackageDao;
    private final TypeApprovalBaselineRepository typeApprovalBaselineRepository;
    private final MdmRxswinRegistryService mdmRxswinRegistryService;
    private final SwinManagedSystemMapper swinManagedSystemMapper;

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
    public List<ActivityUpgradeTargetPo> listUpgradeTarget(Long activityId) {
        return activityUpgradeTargetDao.selectPoByActivityId(activityId);
    }

    /**
     * 获取升级活动下的固定配置字列表
     *
     * @param activityId 升级活动ID
     * @return 固定配置字列表
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
    public int createActivity(ActivityPo activity, List<ActivityUpgradeTargetPo> activityUpgradeTargetList) {
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
        if (activityUpgradeTargetList != null && !activityUpgradeTargetList.isEmpty()) {
            int seq = 1;
            for (ActivityUpgradeTargetPo po : activityUpgradeTargetList) {
                po.setActivityId(activity.getId());
                // 如果source_type未设置，默认为手动
                if (po.getSourceType() == null) {
                    po.setSourceType(0);
                }
                // 如果install_seq未设置，默认递增
                if (po.getInstallSeq() == null) {
                    po.setInstallSeq(seq++);
                }
            }
            activityUpgradeTargetDao.batchInsertPo(activityUpgradeTargetList);
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
    public int createUpgradeTarget(Long activityId, Long[] softwareBuildVersionIds) {
        List<ActivityUpgradeTargetPo> existingTargets = listUpgradeTarget(activityId);
        Set<Long> existingSoftwareBuildVersionIds = existingTargets.stream()
                .map(ActivityUpgradeTargetPo::getSoftwareBuildVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int maxInstallSeq = existingTargets.stream()
                .map(ActivityUpgradeTargetPo::getInstallSeq)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        List<ActivityUpgradeTargetPo> list = new ArrayList<>();
        for (Long softwareBuildVersionId : softwareBuildVersionIds) {
            if (!existingSoftwareBuildVersionIds.contains(softwareBuildVersionId)) {
                if (!softwareBuildVersionAppService.checkReleaseGate(softwareBuildVersionId)) {
                    throw new IllegalStateException("软件内部版本[" + softwareBuildVersionId + "]未通过发布门禁校验（版本需RELEASED且引用软件包均ACTIVE）");
                }
                list.add(ActivityUpgradeTargetPo.builder()
                        .activityId(activityId)
                        .softwareBuildVersionId(softwareBuildVersionId)
                        .sourceType(0) // 手动
                        .critical(false)
                        .ota(true)
                        .installSeq(++maxInstallSeq)
                        .groupNo(0)
                        .forceUpgrade(false)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            return activityUpgradeTargetDao.batchInsertPo(list);
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
     * @param activityId 升级活动ID
     * @param list       升级活动升级对象列表
     * @return 结果
     */
    public int modifyUpgradeTarget(Long activityId, List<ActivityUpgradeTargetPo> list) {
        AtomicInteger result = new AtomicInteger();
        listUpgradeTarget(activityId).forEach(po -> {
            list.forEach(target -> {
                if (po.getId().longValue() == target.getId()) {
                    boolean changed = false;
                    if (!Objects.equals(po.getSoftwareBuildVersionId(), target.getSoftwareBuildVersionId())) {
                        po.setSoftwareBuildVersionId(target.getSoftwareBuildVersionId());
                        changed = true;
                    }
                    if (!Objects.equals(po.getForceUpgrade(), target.getForceUpgrade())) {
                        po.setForceUpgrade(target.getForceUpgrade());
                        changed = true;
                    }
                    if (changed) {
                        activityUpgradeTargetDao.updatePo(po);
                        result.getAndIncrement();
                    }
                }
            });
        });
        return result.get();
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
    public int deleteUpgradeTarget(Long activityId, Long[] ids) {
        return activityUpgradeTargetDao.batchPhysicalDeletePoByActivityIdAndIds(activityId, ids);
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
    public int regroupUpgradeTarget(Long activityId, List<ActivityUpgradeTargetPo> list) {
        AtomicInteger result = new AtomicInteger();
        listUpgradeTarget(activityId).forEach(po -> {
            list.forEach(target -> {
                if (po.getId().longValue() == target.getId()) {
                    if (!Objects.equals(po.getGroupNo(), target.getGroupNo())) {
                        po.setGroupNo(target.getGroupNo());
                        po.setInstallSeq(0);
                        activityUpgradeTargetDao.updatePo(po);
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
    public int resortUpgradeTarget(Long activityId, List<ActivityUpgradeTargetPo> list) {
        AtomicInteger result = new AtomicInteger();
        listUpgradeTarget(activityId).forEach(po -> {
            list.forEach(target -> {
                if (po.getId().longValue() == target.getId()) {
                    if (po.getInstallSeq() == null || po.getInstallSeq().intValue() != target.getInstallSeq()) {
                        po.setInstallSeq(target.getInstallSeq());
                        activityUpgradeTargetDao.updatePo(po);
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
    public int countUpgradeTarget(Long activityId) {
        return activityUpgradeTargetDao.countByActivityId(activityId);
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
                .createTime(new Date())
                .build();
        activityApprovalDao.insert(approval);

        ActivityPo activity = activityDao.selectPoById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在: " + activityId);
        }

        if ("REJECT".equals(result)) {
            // 任一级驳回，回到编辑态
            activity.setState(ActivityState.REJECTED.value);
            activityDao.updatePo(activity);
            log.info("活动[{}]在[{}]阶段被驳回", activityId, approvalStage);
        } else if ("PASS".equals(result)) {
            // 检查是否所有三级审批都已通过
            if (isAllApprovalStagesPassed(activityId)) {
                // 所有三级审批通过，自动跃迁到 APPROVED
                activity.setState(ActivityState.APPROVED.value);
                activityDao.updatePo(activity);
                log.info("活动[{}]所有三级审批通过，状态自动跃迁到APPROVED", activityId);
            } else {
                log.info("活动[{}]在[{}]阶段通过，等待后续审批", activityId, approvalStage);
            }
        }

        return approval;
    }

    /**
     * 检查活动是否所有三级审批（QUALITY、PRODUCT、SECURITY）都已通过
     *
     * @param activityId 升级活动ID
     * @return true: 所有阶段都已通过，false: 还有未通过的阶段
     */
    private boolean isAllApprovalStagesPassed(Long activityId) {
        List<ActivityApprovalPo> approvals = activityApprovalDao.selectByActivityId(activityId);
        Set<String> passedStages = approvals.stream()
                .filter(a -> "PASS".equals(a.getResult()))
                .map(ActivityApprovalPo::getApprovalStage)
                .collect(Collectors.toSet());
        
        // 检查三个阶段是否都已通过
        return passedStages.contains(ApprovalLevel.QUALITY.value)
                && passedStages.contains(ApprovalLevel.PRODUCT.value)
                && passedStages.contains(ApprovalLevel.SECURITY.value);
    }

    /**
     * 型式批准影响评估
     * 基于TA基线投影（tb_type_approval_baseline*）判定「上一批准基准」
     * 评估结果写入 type_approval_assessment_state
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

        try {
            // 1. 获取活动关联的SWIN代码（从SWIN受管系统清单中查询）
            LambdaQueryWrapper<SwinManagedSystemPo> swinQuery = new LambdaQueryWrapper<>();
            swinQuery.eq(SwinManagedSystemPo::getIsTypeApprovalRelevant, true)
                     .last("LIMIT 1");
            SwinManagedSystemPo swinManagedSystem = swinManagedSystemMapper.selectOne(swinQuery);

            String swinCode = swinManagedSystem != null ? swinManagedSystem.getSwinCode() : null;
            if (swinCode == null || swinCode.isEmpty()) {
                log.warn("活动[{}]未关联SWIN代码，无法进行型批评估", activityId);
                activity.setTypeApprovalAssessmentState(TypeApprovalAssessmentState.BLOCKED.value);
                activityDao.updatePo(activity);
                return TypeApprovalAssessmentState.BLOCKED;
            }

            // 2. 读取TA基线投影作为「上一批准基准」
            List<TypeApprovalBaseline> taBaselines = typeApprovalBaselineRepository.listBySwinCode(swinCode);
            if (taBaselines == null || taBaselines.isEmpty()) {
                log.warn("活动[{}]关联的SWIN[{}]无TA基线投影，fail-safe阻断发布", activityId, swinCode);
                activity.setTypeApprovalAssessmentState(TypeApprovalAssessmentState.BLOCKED.value);
                activityDao.updatePo(activity);
                return TypeApprovalAssessmentState.BLOCKED;
            }

            // 3. 计算活动目标版本组合的digest
            String activityDigest = calculateActivityTargetDigest(activityId);

            // 4. 比对digest与TA基线的projection_digest
            boolean digestMatch = false;
            for (TypeApprovalBaseline taBaseline : taBaselines) {
                if (activityDigest != null && activityDigest.equals(taBaseline.getProjectionDigest())) {
                    digestMatch = true;
                    log.info("活动[{}]目标组合digest与TA基线[{}]一致，未越型批边界", activityId, taBaseline.getTaBaselineCode());
                    break;
                }
            }

            if (digestMatch) {
                // digest一致，未越型批边界，可跳过manifest冻结与RXSWIN迭代
                activity.setTypeApprovalAssessmentState(TypeApprovalAssessmentState.PASSED.value);
                activityDao.updatePo(activity);
                return TypeApprovalAssessmentState.PASSED;
            }

            // 5. digest不一致，需要冻结manifest并请求MDM生成RXSWIN
            log.info("活动[{}]目标组合digest与TA基线不一致，需冻结manifest并生成RXSWIN", activityId);

            // 冻结 tb_approved_sw_manifest 版本组合快照
            ApprovedSwManifestPo manifest = freezeManifest(activityId, swinCode, activityDigest);

            // 以 manifest_code 请求 MDM(EEAD) 幂等生成并同步返回 RXSWIN
            RxswinRegisterRequest registerRequest = RxswinRegisterRequest.builder()
                    .manifestCode(manifest.getManifestCode())
                    .swinCode(swinCode)
                    .manifestDigest(activityDigest)
                    .approvedAt(new Date())
                    .build();

            RxswinRegistryResponse registryResponse = mdmRxswinRegistryService.register(registerRequest);
            if (registryResponse != null && registryResponse.getRxswinValue() != null) {
                // 回填 rxswin_value 到 manifest
                manifest.setRxswinValue(registryResponse.getRxswinValue());
                approvedSwManifestDao.updateById(manifest);

                // 回填到活动
                activity.setRxswin(registryResponse.getRxswinValue());
                activity.setTypeApprovalAssessmentState(TypeApprovalAssessmentState.PASSED.value);
                activityDao.updatePo(activity);
                return TypeApprovalAssessmentState.PASSED;
            } else {
                log.error("活动[{}]请求MDM生成RXSWIN失败", activityId);
                activity.setTypeApprovalAssessmentState(TypeApprovalAssessmentState.BLOCKED.value);
                activityDao.updatePo(activity);
                return TypeApprovalAssessmentState.BLOCKED;
            }
        } catch (Exception e) {
            log.error("活动[{}]型式批准评估异常: {}", activityId, e.getMessage(), e);
            activity.setTypeApprovalAssessmentState(TypeApprovalAssessmentState.BLOCKED.value);
            activityDao.updatePo(activity);
            return TypeApprovalAssessmentState.BLOCKED;
        }
    }

    /**
     * 计算活动目标版本组合的digest
     * 按与MDM一致的规范化口径 sha256(sortedItems) 计算
     */
    private String calculateActivityTargetDigest(Long activityId) {
        List<ActivityUpgradeTargetPo> targets = activityUpgradeTargetDao.selectPoByActivityId(activityId);
        if (targets == null || targets.isEmpty()) {
            return null;
        }

        // 按 vehicle_node_code + part_code 排序
        targets.sort((a, b) -> {
            int cmp = String.valueOf(a.getVehicleNodeCode()).compareTo(String.valueOf(b.getVehicleNodeCode()));
            if (cmp != 0) return cmp;
            return String.valueOf(a.getPartCode()).compareTo(String.valueOf(b.getPartCode()));
        });

        // 构建规范化字符串
        StringBuilder sb = new StringBuilder();
        for (ActivityUpgradeTargetPo target : targets) {
            sb.append(target.getVehicleNodeCode()).append(":").append(target.getPartCode()).append(":");
            if (target.getSoftwareBuildVersionId() != null) {
                sb.append(target.getSoftwareBuildVersionId());
            }
            sb.append(";");
        }

        // 计算SHA-256
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("计算digest失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 冻结版本组合快照到 tb_approved_sw_manifest
     */
    private ApprovedSwManifestPo freezeManifest(Long activityId, String swinCode, String digest) {
        String manifestCode = "MFS-" + activityId + "-" + System.currentTimeMillis();

        ApprovedSwManifestPo manifest = new ApprovedSwManifestPo();
        manifest.setManifestCode(manifestCode);
        manifest.setActivityId(activityId);
        manifest.setSwinCode(swinCode);
        manifest.setManifestStatus("FROZEN");
        manifest.setApproveTime(new Date());
        manifest.setCreateTime(new Date());
        manifest.setModifyTime(new Date());
        approvedSwManifestDao.insert(manifest);

        // 保存明细
        List<ActivityUpgradeTargetPo> targets = activityUpgradeTargetDao.selectPoByActivityId(activityId);
        if (targets != null) {
            for (ActivityUpgradeTargetPo target : targets) {
                ApprovedSwManifestItemPo item = new ApprovedSwManifestItemPo();
                item.setManifestId(manifest.getId());
                item.setVehicleNodeCode(target.getVehicleNodeCode());
                item.setPartCode(target.getPartCode());
                // 获取软件版本号
                if (target.getSoftwareBuildVersionId() != null) {
                    SoftwareBuildVersionPo sbv = softwareBuildVersionAppService.getSoftwareBuildVersionById(target.getSoftwareBuildVersionId());
                    item.setApprovedVersion(sbv != null ? sbv.getSoftwareBuildVer() : "TBD");
                }
                item.setCreateTime(new Date());
                item.setModifyTime(new Date());
                approvedSwManifestItemDao.insert(item);
            }
        }

        return manifest;
    }

    /**
     * 刷新活动总文件大小（只读缓存）
     *
     * @param activityId 升级活动ID
     */
    public void refreshTotalFileSize(Long activityId) {
        List<ActivityUpgradeTargetPo> targetList = activityUpgradeTargetDao.selectPoByActivityId(activityId);
        long totalSize = 0L;
        Set<Long> countedPackageIds = new HashSet<>();
        for (ActivityUpgradeTargetPo target : targetList) {
            if (target.getSoftwareBuildVersionId() == null) {
                continue;
            }
            List<SoftwareBuildVersionPackagePo> versionPackages = softwareBuildVersionPackageDao.selectPoBySoftwareBuildVersionId(target.getSoftwareBuildVersionId());
            for (SoftwareBuildVersionPackagePo vp : versionPackages) {
                if (countedPackageIds.add(vp.getSoftwarePackageId())) {
                    SoftwarePackagePo pkg = softwarePackageDao.selectPoById(vp.getSoftwarePackageId());
                    if (pkg != null && pkg.getPackageSize() != null) {
                        totalSize += pkg.getPackageSize();
                    }
                }
            }
        }
        ActivityPo update = new ActivityPo();
        update.setId(activityId);
        update.setTotalFileSize(totalSize);
        update.setSizeCalcTime(new Date());
        activityDao.updatePo(update);
    }

    // ==================== A1. 目标版本组合 ====================

    // 目标版本组合功能已并入升级对象表，相关方法已移除

    // ==================== C1. 安装顺序 ====================

    // 安装顺序功能已并入升级对象表，相关方法已移除

    // ==================== C2. 同升同降依赖组 ====================

    public List<ActivityGroupPolicyPo> listGroupPolicy(Long activityId) {
        return activityGroupPolicyDao.selectByActivityId(activityId);
    }

    public int saveGroupPolicy(ActivityGroupPolicyPo po) {
        if (po.getId() == null) {
            return activityGroupPolicyDao.insertPo(po);
        }
        return activityGroupPolicyDao.updatePo(po);
    }

    public int deleteGroupPolicy(Long id) {
        return activityGroupPolicyDao.logicalDeletePo(id);
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
