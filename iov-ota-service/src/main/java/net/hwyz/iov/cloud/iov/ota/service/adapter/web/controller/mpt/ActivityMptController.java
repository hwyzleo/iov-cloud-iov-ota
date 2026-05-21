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
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.CompatiblePnExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ActivityAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.CompatiblePnAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.ActivityNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ActivityRepository;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.BaselineSoftwareBuildVersionExServiceAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityCompatiblePnPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityFixedConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivitySoftwareBuildVersionPo;
import net.hwyz.iov.cloud.ota.baseline.api.contract.BaselineSoftwareBuildVersionExService;
import net.hwyz.iov.cloud.ota.baseline.api.feign.service.ExBaselineService;
import net.hwyz.iov.cloud.iov.ota.api.vo.CompatiblePnExService;
import net.hwyz.iov.cloud.ota.baseline.api.contract.BaselineSoftwareBuildVersionExService;
import net.hwyz.iov.cloud.ota.baseline.api.feign.service.ExBaselineService;
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
@RequestMapping(value = "/mpt/activity")
public class ActivityMptController extends BaseController {

    private final CacheService cacheService;
    private final ExBaselineService exBaselineService;
    private final ActivityAppService activityAppService;
    private final ActivityRepository activityRepository;
    private final CompatiblePnAppService compatiblePnAppService;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;

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
        if (group == null) {
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
                mpt.setSoftwareSource(softwareBuildVersion.getSoftwareSource() != null ? Integer.parseInt(softwareBuildVersion.getSoftwareSource()) : null);
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
        mptList.forEach(mpt -> {
        });
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
        if (activityPo.getBaseline()) {
            List<BaselineSoftwareBuildVersionExService> baselineSoftwareBuildVersionList = exBaselineService.listSoftwareBuildVersion(activityPo.getBaselineCode());
            activitySoftwareBuildVersionPoList = BaselineSoftwareBuildVersionExServiceAssembler.INSTANCE.toPoList(baselineSoftwareBuildVersionList);
        }
        return ApiResponse.ok(activityAppService.createActivity(activityPo, activitySoftwareBuildVersionPoList));
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
}
