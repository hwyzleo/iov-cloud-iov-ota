package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.mpt;

import cn.hutool.core.util.ObjUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mdm.api.service.MdmPartService;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.PartResponse;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.iov.ota.api.vo.*;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityFixedConfigWordMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityUpgradeTargetPo;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityApprovalMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ApprovedSwManifestMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.RegulatoryFilingMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ActivityAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import java.util.Date;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.ActivityNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.BaselineNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.BaselineItem;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ActivityRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.BaselineItemRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.BaselineRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityFixedConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityApprovalPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityGroupPolicyPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.RegulatoryFilingPo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 升级活动相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/activity/v1")
public class MptActivityController extends BaseController {

    private final CacheService cacheService;
    private final MdmPartService mdmPartService;
    private final ActivityAppService activityAppService;
    private final ActivityRepository activityRepository;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;
    private final BaselineRepository baselineRepository;
    private final BaselineItemRepository baselineItemRepository;

    /**
     * 分页查询升级活动
     *
     * @param activity 升级活动
     * @return 升级活动列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<ActivityMpt>> list(ActivityMpt activity) {
        log.info("管理后台用户[{}]分页查询升级活动", SecurityUtils.getUsername());
        startPage();
        List<ActivityPo> activityPoList = activityAppService.search(activity.getName(), activity.getState(),
                getBeginTime(activity), getEndTime(activity));
        List<ActivityMpt> activityMptList = PageUtil.convert(activityPoList, ActivityMptAssembler.INSTANCE::fromPo);
        activityMptList.forEach(activityMpt -> activityMpt.setUpgradeTargetCount(activityAppService.countUpgradeTarget(activityMpt.getId())));
        return ApiResponse.ok(getPageResult(activityMptList));
    }

    /**
     * 获取所有升级活动状态
     *
     * @return 升级活动状态列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/listAllActivityState")
    public ApiResponse<List<Map<String, Object>>> listAllActivityState() {
        log.info("管理后台用户[{}]获取所有升级活动状态", SecurityUtils.getUsername());
        List<Map<String, Object>> list = new ArrayList<>();
        for (ActivityState activityState : ActivityState.values()) {
            list.add(Map.of("value", activityState.value, "label", activityState.label));
        }
        return ApiResponse.ok(list);
    }

    /**
     * 列出升级活动下升级对象
     *
     * @param activityId 升级活动ID
     * @param group      组
     * @return 升级对象列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listUpgradeTarget")
    public ApiResponse<Map<String, Object>> listUpgradeTarget(@PathVariable Long activityId, @RequestParam(required = false) Integer group) {
        log.info("管理后台用户[{}]列出升级活动[{}]下升级对象", SecurityUtils.getUsername(), activityId);
        List<ActivityUpgradeTargetPo> poList = activityAppService.listUpgradeTarget(activityId);
        Set<Integer> groupSet = poList.stream().map(ActivityUpgradeTargetPo::getGroupNo).collect(Collectors.toSet());
        if (group == null && !groupSet.isEmpty()) {
            group = groupSet.iterator().next();
        }
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (ActivityUpgradeTargetPo po : poList) {
            if (po.getGroupNo() != null && po.getGroupNo().intValue() == group) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", po.getId());
                item.put("activityId", po.getActivityId());
                item.put("sourceType", po.getSourceType());
                item.put("baselineCode", po.getBaselineCode());
                item.put("vehicleNodeCode", po.getVehicleNodeCode());
                item.put("partCode", po.getPartCode());
                item.put("softwareBuildVersionId", po.getSoftwareBuildVersionId());
                item.put("critical", po.getCritical());
                item.put("ota", po.getOta());
                item.put("installSeq", po.getInstallSeq());
                item.put("parallelGroup", po.getParallelGroup());
                item.put("groupNo", po.getGroupNo());
                item.put("forceUpgrade", po.getForceUpgrade());
                if (po.getSoftwareBuildVersionId() != null) {
                    SoftwareBuildVersionPo softwareBuildVersion = softwareBuildVersionAppService.getSoftwareBuildVersionById(po.getSoftwareBuildVersionId());
                    item.put("deviceCode", softwareBuildVersion.getDeviceCode());
                    item.put("softwarePn", softwareBuildVersion.getSoftwarePn());
                    PartResponse part = mdmPartService.getByCode(softwareBuildVersion.getSoftwarePn());
                    if (ObjUtil.isNotNull(part)) {
                        item.put("softwarePartName", part.getName());
                    }
                    item.put("softwareBuildVer", softwareBuildVersion.getSoftwareBuildVer());
                    item.put("softwareSource", softwareBuildVersion.getSoftwareSource());
                }
                resultList.add(item);
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("group", group);
        map.put("groups", groupSet);
        map.put("list", resultList);
        return ApiResponse.ok(map);
    }

    /**
     * 列出升级活动下固定配置字
     *
     * @param activityId 升级活动ID
     * @return 固定配置字列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listFixedConfigWord")
    public ApiResponse<List<ActivityFixedConfigWordMpt>> listFixedConfigWord(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]列出升级活动[{}]下固定配置字", SecurityUtils.getUsername(), activityId);
        List<ActivityFixedConfigWordPo> poList = activityAppService.listFixedConfigWord(activityId);
        List<ActivityFixedConfigWordMpt> mptList = ActivityFixedConfigWordMptAssembler.INSTANCE.fromPoList(poList);
        return ApiResponse.ok(mptList);
    }

    /**
     * 导出升级活动
     *
     * @param response 响应
     * @param activity 升级活动
     */
    @Log(title = "升级活动管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:activity:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ActivityMpt activity) {
        log.info("管理后台用户[{}]导出升级活动", SecurityUtils.getUsername());
    }

    /**
     * 根据升级活动ID获取升级活动
     *
     * @param activityId 升级活动ID
     * @return 升级活动
     */
    @RequiresPermissions("ota:fota:activity:query")
    @GetMapping(value = "/{activityId}")
    public ApiResponse<ActivityMpt> getInfo(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]根据升级活动ID[{}]获取升级活动", SecurityUtils.getUsername(), activityId);
        ActivityPo activityPo = activityAppService.getActivityById(activityId);
        return ApiResponse.ok(ActivityMptAssembler.INSTANCE.fromPo(activityPo));
    }

    /**
     * 新增升级活动
     *
     * @param activity 升级活动
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:fota:activity:add")
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody ActivityMpt activity) {
        log.info("管理后台用户[{}]新增升级活动[{}]", SecurityUtils.getUsername(), activity.getName());
        ActivityPo activityPo = ActivityMptAssembler.INSTANCE.toPo(activity);
        activityPo.setCreateBy(SecurityUtils.getUserId().toString());
        List<ActivityUpgradeTargetPo> activityUpgradeTargetList = null;
        if (activityPo.getBaseline()) {
            baselineRepository.getByBaselineCode(activityPo.getBaselineCode())
                    .orElseThrow(() -> new BaselineNotExistException(activityPo.getBaselineCode()));
            List<BaselineItem> baselineItems = baselineItemRepository.listByBaselineCode(activityPo.getBaselineCode());
            activityUpgradeTargetList = new ArrayList<>();
            int seq = 1;
            for (BaselineItem item : baselineItems) {
                activityUpgradeTargetList.add(ActivityUpgradeTargetPo.builder()
                        .sourceType(1)
                        .baselineCode(activityPo.getBaselineCode())
                        .vehicleNodeCode(item.getVehicleNodeCode())
                        .partCode(item.getPartCode())
                        .installSeq(seq++)
                        .groupNo(0)
                        .build());
            }
        }
        return ApiResponse.ok(activityAppService.createActivity(activityPo, activityUpgradeTargetList));
    }

    /**
     * 新增关联的升级对象
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/addUpgradeTarget/{softwareBuildVersionIds}")
    public ApiResponse<Integer> addUpgradeTarget(@PathVariable Long activityId, @PathVariable Long[] softwareBuildVersionIds) {
        log.info("管理后台用户[{}]新增升级活动[{}]关联的升级对象[{}]", SecurityUtils.getUsername(), activityId, softwareBuildVersionIds);
        return ApiResponse.ok(activityAppService.createUpgradeTarget(activityId, softwareBuildVersionIds));
    }

    /**
     * 新增关联的固定配置字
     *
     * @param activityId         升级活动ID
     * @param fixedConfigWordIds 固定配置字ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/addFixedConfigWord/{fixedConfigWordIds}")
    public ApiResponse<Integer> addFixedConfigWord(@PathVariable Long activityId, @PathVariable Long[] fixedConfigWordIds) {
        log.info("管理后台用户[{}]新增升级活动[{}]关联的固定配置字[{}]", SecurityUtils.getUsername(), activityId, fixedConfigWordIds);
        return ApiResponse.ok(activityAppService.createFixedConfigWord(activityId, fixedConfigWordIds));
    }

    /**
     * 修改保存升级活动
     *
     * @param activity 升级活动
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody ActivityMpt activity) {
        log.info("管理后台用户[{}]修改保存升级活动[{}]", SecurityUtils.getUsername(), activity.getName());
        ActivityPo activityPo = ActivityMptAssembler.INSTANCE.toPo(activity);
        activityPo.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(activityAppService.modifyActivity(activityPo));
    }

    /**
     * 修改关联的升级对象
     *
     * @param activityId 升级活动ID
     * @param list       升级活动升级对象列表
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/editUpgradeTarget")
    public ApiResponse<Integer> editUpgradeTarget(@PathVariable Long activityId, @Validated @RequestBody List<ActivityUpgradeTargetPo> list) {
        log.info("管理后台用户[{}]修改升级活动[{}]关联的升级对象", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(activityAppService.modifyUpgradeTarget(activityId, list));
    }

    /**
     * 提交升级活动
     *
     * @param activityId 升级活动ID
     * @param activity   升级活动
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:submit")
    @PostMapping("/{activityId}/action/submit")
    public ApiResponse<Integer> submit(@PathVariable Long activityId, @Validated @RequestBody ActivityMpt activity) {
        log.info("管理后台用户[{}]提交升级活动[{}]", SecurityUtils.getUsername(), activityId);
        if (activity == null) {
            activity = ActivityMpt.builder().build();
        }
        activity.setId(activityId);
        ActivityPo activityPo = ActivityMptAssembler.INSTANCE.toPo(activity);
        activityPo.setModifyBy(SecurityUtils.getUserId().toString());
        ActivityDo activityDo = activityRepository.getById(activityPo.getId()).orElseThrow(() -> new ActivityNotExistException(activityPo.getId()));
        int result = activityDo.submit(activityPo);
        activityRepository.save(activityDo);
        return ApiResponse.ok(result);
    }

    /**
     * 发布升级活动
     *
     * @param activityId 升级活动ID
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:release")
    @PostMapping("/{activityId}/action/release")
    public ApiResponse<Integer> release(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]发布升级活动[{}]", SecurityUtils.getUsername(), activityId);
        ActivityDo activityDo = activityRepository.getById(activityId).orElseThrow(() -> new ActivityNotExistException(activityId));
        int result = activityDo.release();
        activityRepository.save(activityDo);
        cacheService.addReleaseActivity(activityDo);
        return ApiResponse.ok(result);
    }

    /**
     * 取消升级活动
     *
     * @param activityId 升级活动ID
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:cancel")
    @PostMapping("/{activityId}/action/cancel")
    public ApiResponse<Integer> cancel(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]取消升级活动[{}]", SecurityUtils.getUsername(), activityId);
        ActivityDo activityDo = activityRepository.getById(activityId).orElseThrow(() -> new ActivityNotExistException(activityId));
        int result = activityDo.cancel();
        activityRepository.save(activityDo);
        cacheService.removeReleaseActivity(activityDo);
        return ApiResponse.ok(result);
    }

    /**
     * 删除升级活动
     *
     * @param activityIds 升级活动ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:activity:remove")
    @DeleteMapping("/{activityIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] activityIds) {
        log.info("管理后台用户[{}]删除升级活动[{}]", SecurityUtils.getUsername(), activityIds);
        return ApiResponse.ok(activityAppService.deleteActivityByIds(activityIds));
    }

    /**
     * 删除关联的升级对象
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本关联ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/removeUpgradeTarget/{softwareBuildVersionIds}")
    public ApiResponse<Integer> removeUpgradeTarget(@PathVariable Long activityId, @PathVariable Long[] softwareBuildVersionIds) {
        log.info("管理后台用户[{}]删除升级活动[{}]关联的升级对象[{}]", SecurityUtils.getUsername(), activityId, softwareBuildVersionIds);
        return ApiResponse.ok(activityAppService.deleteUpgradeTarget(activityId, softwareBuildVersionIds));
    }

    /**
     * 删除关联的固定配置字
     *
     * @param activityId         升级活动ID
     * @param fixedConfigWordIds 固定配置字ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/removeFixedConfigWord/{fixedConfigWordIds}")
    public ApiResponse<Integer> removeFixedConfigWord(@PathVariable Long activityId, @PathVariable Long[] fixedConfigWordIds) {
        log.info("管理后台用户[{}]删除升级活动[{}]关联的固定配置字[{}]", SecurityUtils.getUsername(), activityId, fixedConfigWordIds);
        return ApiResponse.ok(activityAppService.deleteFixedConfigWord(activityId, fixedConfigWordIds));
    }

    /**
     * 调整关联的升级对象组
     *
     * @param activityId 升级活动ID
     * @param list       升级活动升级对象列表
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/regroupUpgradeTarget")
    public ApiResponse<Integer> regroupUpgradeTarget(@PathVariable Long activityId, @Validated @RequestBody List<ActivityUpgradeTargetPo> list) {
        log.info("管理后台用户[{}]调整基线[{}]关联的升级对象组", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(activityAppService.regroupUpgradeTarget(activityId, list));
    }

    /**
     * 重排序关联的升级对象
     *
     * @param activityId 升级活动ID
     * @param list       升级活动升级对象列表
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/resortUpgradeTarget")
    public ApiResponse<Integer> resortUpgradeTarget(@PathVariable Long activityId, @Validated @RequestBody List<ActivityUpgradeTargetPo> list) {
        log.info("管理后台用户[{}]重排序基线[{}]关联的升级对象", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(activityAppService.resortUpgradeTarget(activityId, list));
    }

    /**
     * 查询活动多级审批记录
     *
     * @param activityId 升级活动ID
     * @return 审批记录列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listApproval")
    public ApiResponse<List<ActivityApprovalMpt>> listApproval(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]查询升级活动[{}]审批记录", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(ActivityApprovalMptAssembler.INSTANCE.fromPoList(activityAppService.listApprovals(activityId)));
    }

    /**
     * 多级审批
     * 串行流程：QUALITY -> PRODUCT -> SECURITY
     *
     * @param activityId    升级活动ID
     * @param approvalStage 审批阶段
     * @param result        审批结果 PASS / REJECT
     * @param comment       审批意见
     * @return 审批记录
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:audit")
    @PostMapping("/{activityId}/action/approve")
    public ApiResponse<ActivityApprovalMpt> approve(@PathVariable Long activityId,
                                                    @RequestParam String approvalStage,
                                                    @RequestParam String result,
                                                    @RequestParam(required = false) String comment) {
        log.info("管理后台用户[{}]审批升级活动[{}] 阶段[{}] 结果[{}]", SecurityUtils.getUsername(), activityId, approvalStage, result);
        ActivityApprovalPo po = activityAppService.approveActivity(activityId, approvalStage,
                SecurityUtils.getUserId().toString(), result, comment);
        return ApiResponse.ok(ActivityApprovalMptAssembler.INSTANCE.fromPo(po));
    }

    /**
     * 型式批准影响评估
     *
     * @param activityId 升级活动ID
     * @return 评估状态
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:audit")
    @PostMapping("/{activityId}/action/impactAssessment")
    public ApiResponse<Map<String, Object>> impactAssessment(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]对升级活动[{}]进行型式批准影响评估", SecurityUtils.getUsername(), activityId);
        var state = activityAppService.assessTypeApproval(activityId);
        return ApiResponse.ok(Map.of("typeApprovalAssessmentState", state.value, "label", state.label));
    }

    // ==================== A1. 目标版本组合 ====================

    // ==================== C2. 组策略 ====================

    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listGroupPolicy")
    public ApiResponse<List<ActivityGroupPolicyPo>> listGroupPolicy(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]查询升级活动[{}]组策略", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(activityAppService.listGroupPolicy(activityId));
    }

    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/saveGroupPolicy")
    public ApiResponse<Integer> saveGroupPolicy(@PathVariable Long activityId, @Validated @RequestBody ActivityGroupPolicyPo po) {
        log.info("管理后台用户[{}]保存升级活动[{}]组策略", SecurityUtils.getUsername(), activityId);
        po.setActivityId(activityId);
        return ApiResponse.ok(activityAppService.saveGroupPolicy(po));
    }

    @Log(title = "升级活动管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:activity:edit")
    @DeleteMapping(value = "/{activityId}/action/deleteGroupPolicy/{id}")
    public ApiResponse<Integer> deleteGroupPolicy(@PathVariable Long activityId, @PathVariable Long id) {
        log.info("管理后台用户[{}]删除升级活动[{}]组策略[{}]", SecurityUtils.getUsername(), activityId, id);
        return ApiResponse.ok(activityAppService.deleteGroupPolicy(id));
    }

    // ==================== D1. 型批版本组合快照（只读） ====================

    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listManifest")
    public ApiResponse<List<ApprovedSwManifestMpt>> listManifest(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]查询升级活动[{}]型批版本组合快照", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(ApprovedSwManifestMptAssembler.INSTANCE.fromPoList(activityAppService.listManifest(activityId)));
    }

    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/manifest/{manifestId}")
    public ApiResponse<ApprovedSwManifestMpt> getManifest(@PathVariable Long activityId, @PathVariable Long manifestId) {
        log.info("管理后台用户[{}]查询型批版本组合快照[{}]", SecurityUtils.getUsername(), manifestId);
        ApprovedSwManifestPo po = activityAppService.getManifestById(manifestId);
        ApprovedSwManifestMpt mpt = ApprovedSwManifestMptAssembler.INSTANCE.fromPo(po);
        if (mpt != null) {
            List<ApprovedSwManifestItemPo> items = activityAppService.listManifestItems(manifestId);
            mpt.setItems(items.stream().map(item -> ApprovedSwManifestItemMpt.builder()
                    .id(item.getId())
                    .manifestId(item.getManifestId())
                    .vehicleNodeCode(item.getVehicleNodeCode())
                    .partCode(item.getPartCode())
                    .approvedVersion(item.getApprovedVersion())
                    .build()).collect(Collectors.toList()));
        }
        return ApiResponse.ok(mpt);
    }

    // ==================== D2. 监管备案 ====================

    @RequiresPermissions("ota:fota:filing:list")
    @GetMapping(value = "/{activityId}/listRegulatoryFiling")
    public ApiResponse<List<RegulatoryFilingMpt>> listRegulatoryFiling(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]查询升级活动[{}]监管备案", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(RegulatoryFilingMptAssembler.INSTANCE.fromPoList(activityAppService.listRegulatoryFiling(activityId)));
    }

    @RequiresPermissions("ota:fota:filing:list")
    @GetMapping(value = "/{activityId}/regulatoryFiling/{filingId}")
    public ApiResponse<RegulatoryFilingMpt> getRegulatoryFiling(@PathVariable Long activityId, @PathVariable Long filingId) {
        log.info("管理后台用户[{}]查询监管备案[{}]", SecurityUtils.getUsername(), filingId);
        return ApiResponse.ok(RegulatoryFilingMptAssembler.INSTANCE.fromPo(activityAppService.getRegulatoryFilingById(filingId)));
    }

    @Log(title = "监管备案管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:fota:filing:add")
    @PostMapping(value = "/{activityId}/action/saveRegulatoryFiling")
    public ApiResponse<Integer> saveRegulatoryFiling(@PathVariable Long activityId, @Validated @RequestBody RegulatoryFilingMpt mpt) {
        log.info("管理后台用户[{}]保存升级活动[{}]监管备案", SecurityUtils.getUsername(), activityId);
        RegulatoryFilingPo po = RegulatoryFilingMptAssembler.INSTANCE.toPo(mpt);
        po.setActivityId(activityId);
        if (po.getId() == null) {
            po.setCreateTime(new Date());
            po.setModifyTime(new Date());
            return ApiResponse.ok(activityAppService.createRegulatoryFiling(po));
        }
        return ApiResponse.ok(activityAppService.updateRegulatoryFiling(po));
    }

    @Log(title = "监管备案管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:filing:remove")
    @DeleteMapping(value = "/{activityId}/action/deleteRegulatoryFiling/{filingId}")
    public ApiResponse<Integer> deleteRegulatoryFiling(@PathVariable Long activityId, @PathVariable Long filingId) {
        log.info("管理后台用户[{}]删除监管备案[{}]", SecurityUtils.getUsername(), filingId);
        return ApiResponse.ok(activityAppService.deleteRegulatoryFiling(filingId));
    }
}
