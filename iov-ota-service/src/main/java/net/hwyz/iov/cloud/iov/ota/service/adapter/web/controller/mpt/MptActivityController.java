package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.api.vo.*;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityCompatiblePnMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityFixedConfigWordMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivitySoftwareBuildVersionMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityTargetVersionMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityInstallOrderMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityDependencyGroupMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ActivityApprovalMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ApprovedSwManifestMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.RegulatoryFilingMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.CompatiblePnExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ActivityAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.CompatiblePnAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.ActivityNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.BaselineNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.BaselineItem;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ActivityRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.BaselineItemRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.BaselineRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityCompatiblePnPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityFixedConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityApprovalPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityDependencyGroupPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityInstallOrderPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivitySoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityTargetVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ApprovedSwManifestPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.RegulatoryFilingPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityTargetVersionPo;
import net.hwyz.iov.cloud.iov.ota.api.vo.CompatiblePnExService;
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
    private final ActivityAppService activityAppService;
    private final ActivityRepository activityRepository;
    private final CompatiblePnAppService compatiblePnAppService;
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
        activityMptList.forEach(activityMpt -> activityMpt.setSoftwareBuildVersionCount(activityAppService.countActivitySoftwareBuildVersion(activityMpt.getId())));
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
     * 列出升级活动下软件内部版本
     *
     * @param activityId 升级活动ID
     * @param group      组
     * @return 软件内部版本列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listSoftwareBuildVersion")
    public ApiResponse<Map<String, Object>> listSoftwareBuildVersion(@PathVariable Long activityId, @RequestParam(required = false) Integer group) {
        log.info("管理后台用户[{}]列出升级活动[{}]下软件零件版本", SecurityUtils.getUsername(), activityId);
        List<ActivitySoftwareBuildVersionPo> poList = activityAppService.listSoftwareBuildVersion(activityId);
        Set<Integer> groupSet = poList.stream().map(ActivitySoftwareBuildVersionPo::getVersionGroup).collect(Collectors.toSet());
        if (group == null && !groupSet.isEmpty()) {
            group = groupSet.iterator().next();
        }
        List<ActivitySoftwareBuildVersionMpt> mptList = new ArrayList<>();
        for (ActivitySoftwareBuildVersionPo po : poList) {
            if (po.getVersionGroup().intValue() == group) {
                ActivitySoftwareBuildVersionMpt mpt = ActivitySoftwareBuildVersionMptAssembler.INSTANCE.fromPo(po);
                SoftwareBuildVersionPo softwareBuildVersion = softwareBuildVersionAppService.getSoftwareBuildVersionById(mpt.getSoftwareBuildVersionId());
                SoftwareBuildVersionExService softwareBuildVersionExService = SoftwareBuildVersionExServiceAssembler.INSTANCE.fromPo(softwareBuildVersion);
                mpt.setDeviceCode(softwareBuildVersionExService.getDeviceCode());
                mpt.setSoftwarePn(softwareBuildVersionExService.getSoftwarePn());
                mpt.setSoftwarePartName(softwareBuildVersionExService.getSoftwarePartName());
                mpt.setSoftwarePartVer(softwareBuildVersionExService.getSoftwarePartVer());
                mpt.setSoftwareBuildVer(softwareBuildVersion.getSoftwareBuildVer());
                mpt.setSoftwareSource(softwareBuildVersion.getSoftwareSource());
                mptList.add(mpt);
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("group", group);
        map.put("groups", groupSet);
        map.put("list", mptList);
        return ApiResponse.ok(map);
    }

    /**
     * 列出升级活动下兼容零件号
     *
     * @param activityId 升级活动ID
     * @return 兼容零件号列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listCompatiblePn")
    public ApiResponse<List<ActivityCompatiblePnMpt>> listCompatiblePn(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]列出升级活动[{}]下兼容零件号", SecurityUtils.getUsername(), activityId);
        List<ActivityCompatiblePnPo> poList = activityAppService.listCompatiblePn(activityId);
        List<ActivityCompatiblePnMpt> mptList = ActivityCompatiblePnMptAssembler.INSTANCE.fromPoList(poList);
        mptList.forEach(mpt -> {
            CompatiblePnPo compatiblePn = compatiblePnAppService.getCompatiblePnById(mpt.getCompatiblePnId());
            if (compatiblePn != null) {
                CompatiblePnExService compatiblePnExService = CompatiblePnExServiceAssembler.INSTANCE.fromPo(compatiblePn);
                mpt.setType(compatiblePnExService.getType());
                mpt.setDeviceCode(compatiblePnExService.getDeviceCode());
                mpt.setPn(compatiblePnExService.getPn());
                mpt.setCompatiblePn(compatiblePnExService.getCompatiblePn());
                mpt.setDescription(compatiblePnExService.getDescription());
            }
        });
        return ApiResponse.ok(mptList);
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
        List<ActivitySoftwareBuildVersionPo> activitySoftwareBuildVersionPoList = null;
        List<ActivityTargetVersionPo> activityTargetVersionPoList = null;
        if (activityPo.getBaseline()) {
            baselineRepository.getByBaselineCode(activityPo.getBaselineCode())
                    .orElseThrow(() -> new BaselineNotExistException(activityPo.getBaselineCode()));
            List<BaselineItem> baselineItems = baselineItemRepository.listByBaselineCode(activityPo.getBaselineCode());
            activityTargetVersionPoList = baselineItems.stream()
                    .map(item -> ActivityTargetVersionPo.builder()
                            .baselineCode(activityPo.getBaselineCode())
                            .vehicleNodeCode(item.getVehicleNodeCode())
                            .partCode(item.getPartCode())
                            .build())
                    .collect(Collectors.toList());
        }
        return ApiResponse.ok(activityAppService.createActivity(activityPo, activitySoftwareBuildVersionPoList, activityTargetVersionPoList));
    }

    /**
     * 新增关联的软件内部版本
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/addSoftwareBuildVersion/{softwareBuildVersionIds}")
    public ApiResponse<Integer> addSoftwareBuildVersion(@PathVariable Long activityId, @PathVariable Long[] softwareBuildVersionIds) {
        log.info("管理后台用户[{}]新增升级活动[{}]关联的软件内部版本[{}]", SecurityUtils.getUsername(), activityId, softwareBuildVersionIds);
        return ApiResponse.ok(activityAppService.createSoftwareBuildVersion(activityId, softwareBuildVersionIds));
    }

    /**
     * 新增关联的兼容零件号
     *
     * @param activityId      升级活动ID
     * @param compatiblePnIds 兼容零件号ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/addCompatiblePn/{compatiblePnIds}")
    public ApiResponse<Integer> addCompatiblePn(@PathVariable Long activityId, @PathVariable Long[] compatiblePnIds) {
        log.info("管理后台用户[{}]新增升级活动[{}]关联的兼容零件号[{}]", SecurityUtils.getUsername(), activityId, compatiblePnIds);
        return ApiResponse.ok(activityAppService.createCompatiblePn(activityId, compatiblePnIds));
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
     * 修改关联的软件内部版本
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @param sorts                   排序数组
     * @param groups                  组数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/editSoftwareBuildVersion/{softwareBuildVersionIds}")
    public ApiResponse<Integer> editSoftwareBuildVersion(@PathVariable Long activityId, @PathVariable Long[] softwareBuildVersionIds,
                                                         @RequestParam Integer[] sorts, @RequestParam Integer[] groups) {
        log.info("管理后台用户[{}]修改升级活动[{}]关联的软件内部版本[{}]", SecurityUtils.getUsername(), activityId, softwareBuildVersionIds);
        return ApiResponse.ok(activityAppService.modifyActivitySoftwareBuildVersion(activityId, softwareBuildVersionIds, sorts, groups));
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
     * 审核升级活动
     *
     * @param activityId    升级活动ID
     * @param activityAudit 升级活动审核
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:audit")
    @PostMapping("/{activityId}/action/audit")
    public ApiResponse<Integer> audit(@PathVariable Long activityId, @Validated @RequestBody ActivityAuditMpt activityAudit) {
        log.info("管理后台用户[{}]审核升级活动[{}]", SecurityUtils.getUsername(), activityId);
        ActivityDo activityDo = activityRepository.getById(activityId).orElseThrow(() -> new ActivityNotExistException(activityId));
        int result = activityDo.audit(activityAudit.getAudit(), activityAudit.getReason());
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
     * 删除关联的软件内部版本
     *
     * @param activityId              升级活动ID
     * @param softwareBuildVersionIds 软件内部版本关联ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/removeSoftwareBuildVersion/{softwareBuildVersionIds}")
    public ApiResponse<Integer> removeSoftwareBuildVersion(@PathVariable Long activityId, @PathVariable Long[] softwareBuildVersionIds) {
        log.info("管理后台用户[{}]删除升级活动[{}]关联的软件内部版本[{}]", SecurityUtils.getUsername(), activityId, softwareBuildVersionIds);
        return ApiResponse.ok(activityAppService.deleteSoftwareBuildVersion(activityId, softwareBuildVersionIds));
    }

    /**
     * 删除关联的兼容零件号
     *
     * @param activityId      升级活动ID
     * @param compatiblePnIds 兼容零件号ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/removeCompatiblePn/{compatiblePnIds}")
    public ApiResponse<Integer> removeCompatiblePn(@PathVariable Long activityId, @PathVariable Long[] compatiblePnIds) {
        log.info("管理后台用户[{}]删除升级活动[{}]关联的兼容零件号[{}]", SecurityUtils.getUsername(), activityId, compatiblePnIds);
        return ApiResponse.ok(activityAppService.deleteCompatiblePn(activityId, compatiblePnIds));
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
     * 调整关联的软件内部版本组
     *
     * @param activityId 升级活动ID
     * @param list       升级活动软件内部版本列表
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/regroupSoftwareBuildVersion")
    public ApiResponse<Integer> regroupSoftwareBuildVersion(@PathVariable Long activityId, @Validated @RequestBody List<ActivitySoftwareBuildVersionMpt> list) {
        log.info("管理后台用户[{}]调整基线[{}]关联的软件内部版本组", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(activityAppService.regroupActivitySoftwareBuildVersion(activityId, list));
    }

    /**
     * 重排序关联的软件内部版本
     *
     * @param activityId 升级活动ID
     * @param list       升级活动软件内部版本列表
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/resortSoftwareBuildVersion")
    public ApiResponse<Integer> resortSoftwareBuildVersion(@PathVariable Long activityId, @Validated @RequestBody List<ActivitySoftwareBuildVersionMpt> list) {
        log.info("管理后台用户[{}]重排序基线[{}]关联的软件内部版本", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(activityAppService.resortActivitySoftwareBuildVersion(activityId, list));
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

    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listTargetVersion")
    public ApiResponse<List<ActivityTargetVersionMpt>> listTargetVersion(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]查询升级活动[{}]目标版本组合", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(ActivityTargetVersionMptAssembler.INSTANCE.fromPoList(activityAppService.listTargetVersion(activityId)));
    }

    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/saveTargetVersion")
    public ApiResponse<Integer> saveTargetVersion(@PathVariable Long activityId, @Validated @RequestBody ActivityTargetVersionMpt mpt) {
        log.info("管理后台用户[{}]保存升级活动[{}]目标版本组合", SecurityUtils.getUsername(), activityId);
        ActivityTargetVersionPo po = ActivityTargetVersionMptAssembler.INSTANCE.toPo(mpt);
        po.setActivityId(activityId);
        return ApiResponse.ok(activityAppService.saveTargetVersion(po));
    }

    @Log(title = "升级活动管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:activity:edit")
    @DeleteMapping(value = "/{activityId}/action/deleteTargetVersion/{id}")
    public ApiResponse<Integer> deleteTargetVersion(@PathVariable Long activityId, @PathVariable Long id) {
        log.info("管理后台用户[{}]删除升级活动[{}]目标版本[{}]", SecurityUtils.getUsername(), activityId, id);
        return ApiResponse.ok(activityAppService.deleteTargetVersion(id));
    }

    // ==================== C1. 安装顺序 ====================

    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listInstallOrder")
    public ApiResponse<List<ActivityInstallOrderMpt>> listInstallOrder(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]查询升级活动[{}]安装顺序", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(ActivityInstallOrderMptAssembler.INSTANCE.fromPoList(activityAppService.listInstallOrder(activityId)));
    }

    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/saveInstallOrder")
    public ApiResponse<Integer> saveInstallOrder(@PathVariable Long activityId, @Validated @RequestBody ActivityInstallOrderMpt mpt) {
        log.info("管理后台用户[{}]保存升级活动[{}]安装顺序", SecurityUtils.getUsername(), activityId);
        ActivityInstallOrderPo po = ActivityInstallOrderMptAssembler.INSTANCE.toPo(mpt);
        po.setActivityId(activityId);
        return ApiResponse.ok(activityAppService.saveInstallOrder(po));
    }

    @Log(title = "升级活动管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:activity:edit")
    @DeleteMapping(value = "/{activityId}/action/deleteInstallOrder/{id}")
    public ApiResponse<Integer> deleteInstallOrder(@PathVariable Long activityId, @PathVariable Long id) {
        log.info("管理后台用户[{}]删除升级活动[{}]安装顺序[{}]", SecurityUtils.getUsername(), activityId, id);
        return ApiResponse.ok(activityAppService.deleteInstallOrder(id));
    }

    // ==================== C2. 同升同降依赖组 ====================

    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{activityId}/listDependencyGroup")
    public ApiResponse<List<ActivityDependencyGroupMpt>> listDependencyGroup(@PathVariable Long activityId) {
        log.info("管理后台用户[{}]查询升级活动[{}]同升同降依赖组", SecurityUtils.getUsername(), activityId);
        return ApiResponse.ok(ActivityDependencyGroupMptAssembler.INSTANCE.fromPoList(activityAppService.listDependencyGroup(activityId)));
    }

    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @PostMapping(value = "/{activityId}/action/saveDependencyGroup")
    public ApiResponse<Integer> saveDependencyGroup(@PathVariable Long activityId, @Validated @RequestBody ActivityDependencyGroupMpt mpt) {
        log.info("管理后台用户[{}]保存升级活动[{}]同升同降依赖组", SecurityUtils.getUsername(), activityId);
        ActivityDependencyGroupPo po = ActivityDependencyGroupMptAssembler.INSTANCE.toPo(mpt);
        po.setActivityId(activityId);
        return ApiResponse.ok(activityAppService.saveDependencyGroup(po));
    }

    @Log(title = "升级活动管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:activity:edit")
    @DeleteMapping(value = "/{activityId}/action/deleteDependencyGroup/{id}")
    public ApiResponse<Integer> deleteDependencyGroup(@PathVariable Long activityId, @PathVariable Long id) {
        log.info("管理后台用户[{}]删除升级活动[{}]同升同降依赖组[{}]", SecurityUtils.getUsername(), activityId, id);
        return ApiResponse.ok(activityAppService.deleteDependencyGroup(id));
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
