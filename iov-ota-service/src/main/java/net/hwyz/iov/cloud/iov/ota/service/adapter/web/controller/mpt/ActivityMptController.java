package net.hwyz.iov.cloud.iov.ota.service.facade.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.common.web.domain.AjaxResult;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.ota.baseline.api.contract.BaselineSoftwareBuildVersionExService;
import net.hwyz.iov.cloud.ota.baseline.api.feign.service.ExBaselineService;
import net.hwyz.iov.cloud.iov.ota.api.contract.*;
import net.hwyz.iov.cloud.iov.ota.api.contract.enums.ActivityState;
import net.hwyz.iov.cloud.iov.ota.api.feign.mpt.ActivityMptApi;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ActivityAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.activity.model.ActivityDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.activity.repository.ActivityRepository;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.exception.ActivityNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityCompatiblePnPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityFixedConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivitySoftwareBuildVersionPo;
import net.hwyz.iov.cloud.ota.pota.api.contract.CompatiblePnExService;
import net.hwyz.iov.cloud.ota.pota.api.contract.FixedConfigWordExService;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwareBuildVersionExService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExCompatiblePnService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExFixedConfigWordService;
import net.hwyz.iov.cloud.ota.pota.api.feign.service.ExSoftwareBuildVersionService;
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
    private final ExCompatiblePnService exCompatiblePnService;
    private final ExFixedConfigWordService exFixedConfigWordService;
    private final ExSoftwareBuildVersionService exSoftwareBuildVersionService;

    /**
     * 分页查询升级活动
     *
     * @param activity 升级活动
     * @return 升级活动列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @Override
    @GetMapping(value = "/list")
    public TableDataInfo list(ActivityMpt activity) {
        logger.info("管理后台用户[{}]分页查询升级活动", SecurityUtils.getUsername());
        startPage();
        List<ActivityPo> activityPoList = activityAppService.search(activity.getName(), activity.getState(),
                getBeginTime(activity), getEndTime(activity));
        List<ActivityMpt> activityMptList = ActivityMptAssembler.INSTANCE.fromPoList(activityPoList);
        activityMptList.forEach(activityMpt -> activityMpt.setSoftwareBuildVersionCount(activityAppService.countActivitySoftwareBuildVersion(activityMpt.getId())));
        return getDataTable(activityPoList, activityMptList);
    }

    /**
     * 获取所有升级活动状态
     *
     * @return 升级活动状态列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @Override
    @GetMapping(value = "/listAllActivityState")
    public AjaxResult listAllActivityState() {
        logger.info("管理后台用户[{}]获取所有升级活动状态", SecurityUtils.getUsername());
        List<Map<String, Object>> list = new ArrayList<>();
        for (ActivityState activityState : ActivityState.values()) {
            list.add(Map.of("value", activityState.value, "label", activityState.label));
        }
        return success(list);
    }

    /**
     * 列出升级活动下软件内部版本
     *
     * @param activityId 升级活动ID
     * @param group      组
     * @return 软件内部版本列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @Override
    @GetMapping(value = "/{activityId}/listSoftwareBuildVersion")
    public AjaxResult listSoftwareBuildVersion(@PathVariable Long activityId, @RequestParam(required = false) Integer group) {
        logger.info("管理后台用户[{}]列出升级活动[{}]下软件零件版本", SecurityUtils.getUsername(), activityId);
        List<ActivitySoftwareBuildVersionPo> poList = activityAppService.listSoftwareBuildVersion(activityId);
        Set<Integer> groupSet = poList.stream().map(ActivitySoftwareBuildVersionPo::getVersionGroup).collect(Collectors.toSet());
        if (group == null) {
            group = groupSet.iterator().next();
        }
        List<ActivitySoftwareBuildVersionMpt> mptList = new ArrayList<>();
        for (ActivitySoftwareBuildVersionPo po : poList) {
            if (po.getVersionGroup().intValue() == group) {
                ActivitySoftwareBuildVersionMpt mpt = ActivitySoftwareBuildVersionMptAssembler.INSTANCE.fromPo(po);
                SoftwareBuildVersionExService softwareBuildVersion = exSoftwareBuildVersionService.getInfo(mpt.getSoftwareBuildVersionId());
                mpt.setDeviceCode(softwareBuildVersion.getDeviceCode());
                mpt.setSoftwarePn(softwareBuildVersion.getSoftwarePn());
                mpt.setSoftwarePartName(softwareBuildVersion.getSoftwarePartName());
                mpt.setSoftwarePartVer(softwareBuildVersion.getSoftwarePartVer());
                mpt.setSoftwareBuildVer(softwareBuildVersion.getSoftwareBuildVer());
                mpt.setSoftwareSource(softwareBuildVersion.getSoftwareSource());
                mptList.add(mpt);
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("group", group);
        map.put("groups", groupSet);
        map.put("list", mptList);
        return success(map);
    }

    /**
     * 列出升级活动下兼容零件号
     *
     * @param activityId 升级活动ID
     * @return 兼容零件号列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @Override
    @GetMapping(value = "/{activityId}/listCompatiblePn")
    public AjaxResult listCompatiblePn(@PathVariable Long activityId) {
        logger.info("管理后台用户[{}]列出升级活动[{}]下兼容零件号", SecurityUtils.getUsername(), activityId);
        List<ActivityCompatiblePnPo> poList = activityAppService.listCompatiblePn(activityId);
        List<ActivityCompatiblePnMpt> mptList = ActivityCompatiblePnMptAssembler.INSTANCE.fromPoList(poList);
        mptList.forEach(mpt -> {
            CompatiblePnExService compatiblePn = exCompatiblePnService.getInfo(mpt.getCompatiblePnId());
            mpt.setType(compatiblePn.getType());
            mpt.setDeviceCode(compatiblePn.getDeviceCode());
            mpt.setPn(compatiblePn.getPn());
            mpt.setCompatiblePn(compatiblePn.getCompatiblePn());
            mpt.setDescription(compatiblePn.getDescription());
        });
        return success(mptList);
    }

    /**
     * 列出升级活动下固定配置字
     *
     * @param activityId 升级活动ID
     * @return 固定配置字列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @Override
    @GetMapping(value = "/{activityId}/listFixedConfigWord")
    public AjaxResult listFixedConfigWord(@PathVariable Long activityId) {
        logger.info("管理后台用户[{}]列出升级活动[{}]下固定配置字", SecurityUtils.getUsername(), activityId);
        List<ActivityFixedConfigWordPo> poList = activityAppService.listFixedConfigWord(activityId);
        List<ActivityFixedConfigWordMpt> mptList = ActivityFixedConfigWordMptAssembler.INSTANCE.fromPoList(poList);
        mptList.forEach(mpt -> {
            FixedConfigWordExService fixedConfigWord = exFixedConfigWordService.getInfo(mpt.getFixedConfigWordId());
            mpt.setDeviceCode(fixedConfigWord.getDeviceCode());
            mpt.setSoftwarePn(fixedConfigWord.getSoftwarePn());
            mpt.setType(fixedConfigWord.getType());
            mpt.setDescription(fixedConfigWord.getDescription());
        });
        return success(mptList);
    }

    /**
     * 导出升级活动
     *
     * @param response 响应
     * @param activity 升级活动
     */
    @Log(title = "升级活动管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:activity:export")
    @Override
    @PostMapping("/export")
    public void export(HttpServletResponse response, ActivityMpt activity) {
        logger.info("管理后台用户[{}]导出升级活动", SecurityUtils.getUsername());
    }

    /**
     * 根据升级活动ID获取升级活动
     *
     * @param activityId 升级活动ID
     * @return 升级活动
     */
    @RequiresPermissions("ota:fota:activity:query")
    @Override
    @GetMapping(value = "/{activityId}")
    public AjaxResult getInfo(@PathVariable Long activityId) {
        logger.info("管理后台用户[{}]根据升级活动ID[{}]获取升级活动", SecurityUtils.getUsername(), activityId);
        ActivityPo activityPo = activityAppService.getActivityById(activityId);
        return success(ActivityMptAssembler.INSTANCE.fromPo(activityPo));
    }

    /**
     * 新增升级活动
     *
     * @param activity 升级活动
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:fota:activity:add")
    @Override
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ActivityMpt activity) {
        logger.info("管理后台用户[{}]新增升级活动[{}]", SecurityUtils.getUsername(), activity.getName());
        ActivityPo activityPo = ActivityMptAssembler.INSTANCE.toPo(activity);
        activityPo.setCreateBy(SecurityUtils.getUserId().toString());
        List<ActivitySoftwareBuildVersionPo> activitySoftwareBuildVersionPoList = null;
        if (activityPo.getBaseline()) {
            List<BaselineSoftwareBuildVersionExService> baselineSoftwareBuildVersionList = exBaselineService.listSoftwareBuildVersion(activityPo.getBaselineCode());
            activitySoftwareBuildVersionPoList = BaselineSoftwareBuildVersionExServiceAssembler.INSTANCE.toPoList(baselineSoftwareBuildVersionList);
        }
        return toAjax(activityAppService.createActivity(activityPo, activitySoftwareBuildVersionPoList));
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
    @Override
    @PostMapping(value = "/{activityId}/action/addSoftwareBuildVersion/{softwareBuildVersionIds}")
    public AjaxResult addSoftwareBuildVersion(@PathVariable Long activityId, @PathVariable Long[] softwareBuildVersionIds) {
        logger.info("管理后台用户[{}]新增升级活动[{}]关联的软件内部版本[{}]", SecurityUtils.getUsername(), activityId, softwareBuildVersionIds);
        return toAjax(activityAppService.createSoftwareBuildVersion(activityId, softwareBuildVersionIds));
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
    @Override
    @PostMapping(value = "/{activityId}/action/addCompatiblePn/{compatiblePnIds}")
    public AjaxResult addCompatiblePn(@PathVariable Long activityId, @PathVariable Long[] compatiblePnIds) {
        logger.info("管理后台用户[{}]新增升级活动[{}]关联的兼容零件号[{}]", SecurityUtils.getUsername(), activityId, compatiblePnIds);
        return toAjax(activityAppService.createCompatiblePn(activityId, compatiblePnIds));
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
    @Override
    @PostMapping(value = "/{activityId}/action/addFixedConfigWord/{fixedConfigWordIds}")
    public AjaxResult addFixedConfigWord(@PathVariable Long activityId, @PathVariable Long[] fixedConfigWordIds) {
        logger.info("管理后台用户[{}]新增升级活动[{}]关联的固定配置字[{}]", SecurityUtils.getUsername(), activityId, fixedConfigWordIds);
        return toAjax(activityAppService.createFixedConfigWord(activityId, fixedConfigWordIds));
    }

    /**
     * 修改保存升级活动
     *
     * @param activity 升级活动
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:edit")
    @Override
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ActivityMpt activity) {
        logger.info("管理后台用户[{}]修改保存升级活动[{}]", SecurityUtils.getUsername(), activity.getName());
        ActivityPo activityPo = ActivityMptAssembler.INSTANCE.toPo(activity);
        activityPo.setModifyBy(SecurityUtils.getUserId().toString());
        return toAjax(activityAppService.modifyActivity(activityPo));
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
    @Override
    @PostMapping(value = "/{activityId}/action/editSoftwareBuildVersion/{softwareBuildVersionIds}")
    public AjaxResult editSoftwareBuildVersion(@PathVariable Long activityId, @PathVariable Long[] softwareBuildVersionIds,
                                               @RequestParam Integer[] sorts, @RequestParam Integer[] groups) {
        logger.info("管理后台用户[{}]修改升级活动[{}]关联的软件内部版本[{}]", SecurityUtils.getUsername(), activityId, softwareBuildVersionIds);
        return success(activityAppService.modifyActivitySoftwareBuildVersion(activityId, softwareBuildVersionIds, sorts, groups));
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
    @Override
    @PostMapping("/{activityId}/action/submit")
    public AjaxResult submit(@PathVariable Long activityId, @Validated @RequestBody ActivityMpt activity) {
        logger.info("管理后台用户[{}]提交升级活动[{}]", SecurityUtils.getUsername(), activityId);
        if (activity == null) {
            activity = ActivityMpt.builder().build();
        }
        activity.setId(activityId);
        ActivityPo activityPo = ActivityMptAssembler.INSTANCE.toPo(activity);
        activityPo.setModifyBy(SecurityUtils.getUserId().toString());
        ActivityDo activityDo = activityRepository.getById(activityPo.getId()).orElseThrow(() -> new ActivityNotExistException(activityPo.getId()));
        int result = activityDo.submit(activityPo);
        activityRepository.save(activityDo);
        return toAjax(result);
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
    @Override
    @PostMapping("/{activityId}/action/audit")
    public AjaxResult audit(@PathVariable Long activityId, @Validated @RequestBody ActivityAuditMpt activityAudit) {
        logger.info("管理后台用户[{}]审核升级活动[{}]", SecurityUtils.getUsername(), activityId);
        ActivityDo activityDo = activityRepository.getById(activityId).orElseThrow(() -> new ActivityNotExistException(activityId));
        int result = activityDo.audit(activityAudit.getAudit(), activityAudit.getReason());
        activityRepository.save(activityDo);
        return toAjax(result);
    }

    /**
     * 发布升级活动
     *
     * @param activityId 升级活动ID
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:activity:release")
    @Override
    @PostMapping("/{activityId}/action/release")
    public AjaxResult release(@PathVariable Long activityId) {
        logger.info("管理后台用户[{}]发布升级活动[{}]", SecurityUtils.getUsername(), activityId);
        ActivityDo activityDo = activityRepository.getById(activityId).orElseThrow(() -> new ActivityNotExistException(activityId));
        int result = activityDo.release();
        activityRepository.save(activityDo);
        cacheService.addReleaseActivity(activityDo);
        return toAjax(result);
    }

    /**
     * 取消升级活动
     *
     * @param activityId 升级活动ID
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:cancel")
    @Override
    @PostMapping("/{activityId}/action/cancel")
    public AjaxResult cancel(@PathVariable Long activityId) {
        logger.info("管理后台用户[{}]取消升级活动[{}]", SecurityUtils.getUsername(), activityId);
        ActivityDo activityDo = activityRepository.getById(activityId).orElseThrow(() -> new ActivityNotExistException(activityId));
        int result = activityDo.cancel();
        activityRepository.save(activityDo);
        cacheService.removeReleaseActivity(activityDo);
        return toAjax(result);
    }

    /**
     * 删除升级活动
     *
     * @param activityIds 升级活动ID数组
     * @return 结果
     */
    @Log(title = "升级活动管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:activity:remove")
    @Override
    @DeleteMapping("/{activityIds}")
    public AjaxResult remove(@PathVariable Long[] activityIds) {
        logger.info("管理后台用户[{}]删除升级活动[{}]", SecurityUtils.getUsername(), activityIds);
        return toAjax(activityAppService.deleteActivityByIds(activityIds));
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
    @Override
    @PostMapping(value = "/{activityId}/action/removeSoftwareBuildVersion/{softwareBuildVersionIds}")
    public AjaxResult removeSoftwareBuildVersion(@PathVariable Long activityId, @PathVariable Long[] softwareBuildVersionIds) {
        logger.info("管理后台用户[{}]删除升级活动[{}]关联的软件内部版本[{}]", SecurityUtils.getUsername(), activityId, softwareBuildVersionIds);
        return toAjax(activityAppService.deleteSoftwareBuildVersion(activityId, softwareBuildVersionIds));
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
    @Override
    @PostMapping(value = "/{activityId}/action/removeCompatiblePn/{compatiblePnIds}")
    public AjaxResult removeCompatiblePn(@PathVariable Long activityId, @PathVariable Long[] compatiblePnIds) {
        logger.info("管理后台用户[{}]删除升级活动[{}]关联的兼容零件号[{}]", SecurityUtils.getUsername(), activityId, compatiblePnIds);
        return toAjax(activityAppService.deleteCompatiblePn(activityId, compatiblePnIds));
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
    @Override
    @PostMapping(value = "/{activityId}/action/removeFixedConfigWord/{fixedConfigWordIds}")
    public AjaxResult removeFixedConfigWord(@PathVariable Long activityId, @PathVariable Long[] fixedConfigWordIds) {
        logger.info("管理后台用户[{}]删除升级活动[{}]关联的固定配置字[{}]", SecurityUtils.getUsername(), activityId, fixedConfigWordIds);
        return toAjax(activityAppService.deleteFixedConfigWord(activityId, fixedConfigWordIds));
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
    @Override
    @PostMapping(value = "/{activityId}/action/regroupSoftwareBuildVersion")
    public AjaxResult regroupSoftwareBuildVersion(@PathVariable Long activityId, @Validated @RequestBody List<ActivitySoftwareBuildVersionMpt> list) {
        logger.info("管理后台用户[{}]调整基线[{}]关联的软件内部版本组", SecurityUtils.getUsername(), activityId);
        return toAjax(activityAppService.regroupActivitySoftwareBuildVersion(activityId, list));
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
    @Override
    @PostMapping(value = "/{activityId}/action/resortSoftwareBuildVersion")
    public AjaxResult resortSoftwareBuildVersion(@PathVariable Long activityId, @Validated @RequestBody List<ActivitySoftwareBuildVersionMpt> list) {
        logger.info("管理后台用户[{}]重排序基线[{}]关联的软件内部版本", SecurityUtils.getUsername(), activityId);
        return toAjax(activityAppService.resortActivitySoftwareBuildVersion(activityId, list));
    }
}
