package net.hwyz.iov.cloud.ota.pota.service.facade.mpt;

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
import net.hwyz.iov.cloud.ota.pota.api.contract.ConfigWordMpt;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwareBuildVersionMpt;
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwarePackageMpt;
import net.hwyz.iov.cloud.ota.pota.api.feign.mpt.SoftwareBuildVersionMptApi;
import net.hwyz.iov.cloud.ota.pota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.ota.pota.service.application.service.SoftwarePackageAppService;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.ConfigWordMptAssembler;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.SoftwareBuildVersionMptAssembler;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.SoftwarePackageMptAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.ConfigWordPo;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwarePackagePo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 软件内部版本信息相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/mpt/softwareBuildVersion")
public class SoftwareBuildVersionMptController extends BaseController implements SoftwareBuildVersionMptApi {

    private final SoftwarePackageAppService softwarePackageAppService;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;

    /**
     * 分页查询软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 软件内部版本信息列表
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:list")
    @Override
    @GetMapping(value = "/list")
    public TableDataInfo list(SoftwareBuildVersionMpt softwareBuildVersion) {
        logger.info("管理后台用户[{}]分页查询软件内部版本信息", SecurityUtils.getUsername());
        startPage();
        List<SoftwareBuildVersionPo> softwareBuildVersionPoList = softwareBuildVersionAppService.search(softwareBuildVersion.getDeviceCode(),
                softwareBuildVersion.getSoftwarePn(), null, getBeginTime(softwareBuildVersion), getEndTime(softwareBuildVersion));
        List<SoftwareBuildVersionMpt> softwareBuildVersionMptList = SoftwareBuildVersionMptAssembler.INSTANCE.fromPoList(softwareBuildVersionPoList);
        softwareBuildVersionMptList.forEach(softwareBuildVersionMpt -> {
            softwareBuildVersionMpt.setSoftwarePackageCount(softwareBuildVersionAppService.countPackage(softwareBuildVersionMpt.getId()));
            softwareBuildVersionMpt.setDependencyCount(softwareBuildVersionAppService.countDependency(softwareBuildVersionMpt.getId()));
        });
        return getDataTable(softwareBuildVersionPoList, softwareBuildVersionMptList);
    }

    /**
     * 查询软件内部版本下软件包
     *
     * @param softwareBuildVersionId 软件零件版本ID
     * @param softwarePackage        软件包
     * @return 软件包列表
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:list")
    @Override
    @GetMapping(value = "/{softwareBuildVersionId}/listSoftwarePackage")
    public AjaxResult listSoftwarePackage(@PathVariable Long softwareBuildVersionId, SoftwarePackageMpt softwarePackage) {
        logger.info("管理后台用户[{}]查询软件内部版本[{}]下软件包", SecurityUtils.getUsername(), softwareBuildVersionId);
        List<SoftwarePackagePo> softwarePackagePoList = softwarePackageAppService.search(softwarePackage.getDeviceCode(),
                softwarePackage.getSoftwarePn(), softwarePackage.getPackageCode(), softwarePackage.getPackageName(),
                softwareBuildVersionId, null, null);
        return success(SoftwarePackageMptAssembler.INSTANCE.fromPoList(softwarePackagePoList));
    }

    /**
     * 查询软件内部版本下依赖的软件内部版本
     *
     * @param softwareBuildVersionId 软件零件版本ID
     * @param softwareBuildVersion   软件零件版本信息
     * @return 依赖的软件零件版本列表
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:list")
    @Override
    @GetMapping(value = "/{softwareBuildVersionId}/listDependency")
    public AjaxResult listDependency(@PathVariable Long softwareBuildVersionId, SoftwareBuildVersionMpt softwareBuildVersion) {
        logger.info("管理后台用户[{}]查询软件内部版本[{}]下依赖的软件零件版本", SecurityUtils.getUsername(), softwareBuildVersionId);
        List<SoftwareBuildVersionMpt> list = new ArrayList<>();
        softwareBuildVersionAppService.listDependency(softwareBuildVersionId).forEach(dependencyPo -> {
            SoftwareBuildVersionPo softwareBuildVersionPo = softwareBuildVersionAppService.getSoftwareBuildVersionById(dependencyPo.getDependencySoftwareBuildVersionId());
            SoftwareBuildVersionMpt softwareBuildVersionMpt = SoftwareBuildVersionMptAssembler.INSTANCE.fromPo(softwareBuildVersionPo);
            softwareBuildVersionMpt.setAdaptiveLevel(dependencyPo.getAdaptiveLevel());
            list.add(softwareBuildVersionMpt);
        });
        return success(list);
    }

    /**
     * 查询软件内部版本下配置字
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param configWord             配置字
     * @return 配置字列表
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:list")
    @Override
    @GetMapping(value = "/{softwareBuildVersionId}/listConfigWord")
    public AjaxResult listConfigWord(@PathVariable Long softwareBuildVersionId, ConfigWordMpt configWord) {
        logger.info("管理后台用户[{}]查询软件内部版本[{}]下配置字", SecurityUtils.getUsername(), softwareBuildVersionId);
        List<ConfigWordPo> configWordPoList = softwareBuildVersionAppService.listConfigWord(softwareBuildVersionId);
        return success(ConfigWordMptAssembler.INSTANCE.fromPoList(configWordPoList));
    }

    /**
     * 导出软件内部版本信息
     *
     * @param response             响应
     * @param softwareBuildVersion 软件内部版本信息
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:pota:softwareBuildVersion:export")
    @Override
    @PostMapping("/export")
    public void export(HttpServletResponse response, SoftwareBuildVersionMpt softwareBuildVersion) {
        logger.info("管理后台用户[{}]导出软件零件版本信息", SecurityUtils.getUsername());
    }

    /**
     * 根据软件内部版本信息ID获取软件内部版本信息
     *
     * @param softwareBuildVersionId 软件内部版本信息ID
     * @return 软件内部版本信息
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:query")
    @Override
    @GetMapping(value = "/{softwareBuildVersionId}")
    public AjaxResult getInfo(@PathVariable Long softwareBuildVersionId) {
        logger.info("管理后台用户[{}]根据软件内部版本信息ID[{}]获取软件内部版本信息", SecurityUtils.getUsername(), softwareBuildVersionId);
        SoftwareBuildVersionPo softwareBuildVersionPo = softwareBuildVersionAppService.getSoftwareBuildVersionById(softwareBuildVersionId);
        return success(SoftwareBuildVersionMptAssembler.INSTANCE.fromPo(softwareBuildVersionPo));
    }

    /**
     * 根据软件内部版本信息ID和配置字ID获取配置字信息
     *
     * @param softwareBuildVersionId 软件内部版本信息ID
     * @param configWordId           配置字ID
     * @return 配置字信息
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:query")
    @Override
    @GetMapping(value = "/{softwareBuildVersionId}/configWord/{configWordId}")
    public AjaxResult getConfigWord(@PathVariable Long softwareBuildVersionId, @PathVariable Long configWordId) {
        logger.info("管理后台用户[{}]根据软件内部版本ID[{}]和配置字ID[{}]获取配置字信息", SecurityUtils.getUsername(), softwareBuildVersionId, configWordId);
        ConfigWordPo configWordPo = softwareBuildVersionAppService.getConfigWordById(softwareBuildVersionId, configWordId);
        return success(ConfigWordMptAssembler.INSTANCE.fromPo(configWordPo));
    }

    /**
     * 新增软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:pota:softwareBuildVersion:add")
    @Override
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SoftwareBuildVersionMpt softwareBuildVersion) {
        logger.info("管理后台用户[{}]新增零件[{}]软件内部版本信息[{}]", SecurityUtils.getUsername(), softwareBuildVersion.getSoftwarePn(), softwareBuildVersion.getSoftwareBuildVer());
        if (!softwareBuildVersionAppService.checkDeviceCodeAndSoftwarePnUnique(softwareBuildVersion.getId(), softwareBuildVersion.getDeviceCode(),
                softwareBuildVersion.getSoftwarePn(), softwareBuildVersion.getSoftwareBuildVer())) {
            return error("新增软件内部版本信息'" + softwareBuildVersion.getSoftwarePn() + "'失败，软件内部版本已存在");
        }
        SoftwareBuildVersionPo softwareBuildVersionPo = SoftwareBuildVersionMptAssembler.INSTANCE.toPo(softwareBuildVersion);
        softwareBuildVersionPo.setCreateBy(SecurityUtils.getUserId().toString());
        return toAjax(softwareBuildVersionAppService.createSoftwareBuildVersion(softwareBuildVersionPo));
    }

    /**
     * 新增关联的软件包
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param softwarePackageIds     软件包ID数组
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @Override
    @PostMapping(value = "/{softwareBuildVersionId}/action/addSoftwarePackage/{softwarePackageIds}")
    public AjaxResult addSoftwarePackage(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwarePackageIds) {
        logger.info("管理后台用户[{}]新增软件内部版本[{}]关联的软件包[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwarePackageIds);
        return toAjax(softwareBuildVersionAppService.createPackage(softwareBuildVersionId, softwarePackageIds));
    }

    /**
     * 新增依赖的软件内部版本
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @param adaptiveLevel           适配级别
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @Override
    @PostMapping(value = "/{softwareBuildVersionId}/action/addDependency/{softwareBuildVersionIds}")
    public AjaxResult addDependency(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwareBuildVersionIds, @RequestParam Integer adaptiveLevel) {
        logger.info("管理后台用户[{}]新增软件内部版本[{}]依赖的软件内部版本[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwareBuildVersionIds);
        return toAjax(softwareBuildVersionAppService.createDependency(softwareBuildVersionId, softwareBuildVersionIds, adaptiveLevel));
    }

    /**
     * 新增配置字
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param configWord             配置字
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @Override
    @PostMapping(value = "/{softwareBuildVersionId}/action/addConfigWord")
    public AjaxResult addConfigWord(@PathVariable Long softwareBuildVersionId, @Validated @RequestBody ConfigWordMpt configWord) {
        logger.info("管理后台用户[{}]新增软件内部版本[{}]配置字", SecurityUtils.getUsername(), softwareBuildVersionId);
        return toAjax(softwareBuildVersionAppService.createConfigWord(softwareBuildVersionId, ConfigWordMptAssembler.INSTANCE.toPo(configWord)));
    }

    /**
     * 修改保存软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @Override
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SoftwareBuildVersionMpt softwareBuildVersion) {
        logger.info("管理后台用户[{}]修改保存零件[{}]软件内部版本信息[{}]", SecurityUtils.getUsername(), softwareBuildVersion.getSoftwarePn(), softwareBuildVersion.getSoftwareBuildVer());
        if (!softwareBuildVersionAppService.checkDeviceCodeAndSoftwarePnUnique(softwareBuildVersion.getId(),
                softwareBuildVersion.getDeviceCode(), softwareBuildVersion.getSoftwarePn(), softwareBuildVersion.getSoftwareBuildVer())) {
            return error("修改保存软件内部版本信息'" + softwareBuildVersion.getSoftwarePn() + "'失败，软件内部版本已存在");
        }
        SoftwareBuildVersionPo softwareBuildVersionPo = SoftwareBuildVersionMptAssembler.INSTANCE.toPo(softwareBuildVersion);
        softwareBuildVersionPo.setModifyBy(SecurityUtils.getUserId().toString());
        return toAjax(softwareBuildVersionAppService.modifySoftwareBuildVersion(softwareBuildVersionPo));
    }

    /**
     * 修改保存依赖的软件依赖版本
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @param adaptiveLevel           适配级别
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @Override
    @PostMapping(value = "/{softwareBuildVersionId}/action/editDependency/{softwareBuildVersionIds}")
    public AjaxResult editDependency(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwareBuildVersionIds, @RequestParam Integer adaptiveLevel) {
        logger.info("管理后台用户[{}]修改保存软件内部版本[{}]依赖的软件内部版本[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwareBuildVersionIds);
        return toAjax(softwareBuildVersionAppService.modifyDependency(softwareBuildVersionId, softwareBuildVersionIds, adaptiveLevel));
    }

    /**
     * 修改保存配置字
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param configWord             配置字
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @Override
    @PostMapping(value = "/{softwareBuildVersionId}/action/editConfigWord")
    public AjaxResult editConfigWord(@PathVariable Long softwareBuildVersionId, @Validated @RequestBody ConfigWordMpt configWord) {
        logger.info("管理后台用户[{}]修改保存软件内部版本[{}]配置字[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, configWord.getId());
        return toAjax(softwareBuildVersionAppService.modifyConfigWord(softwareBuildVersionId, ConfigWordMptAssembler.INSTANCE.toPo(configWord)));
    }

    /**
     * 删除软件内部版本信息
     *
     * @param softwareBuildVersionIds 软件内部版本信息ID数组
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:remove")
    @Override
    @DeleteMapping("/{softwareBuildVersionIds}")
    public AjaxResult remove(@PathVariable Long[] softwareBuildVersionIds) {
        logger.info("管理后台用户[{}]删除软件内部版本信息[{}]", SecurityUtils.getUsername(), softwareBuildVersionIds);
        return toAjax(softwareBuildVersionAppService.deleteSoftwareBuildVersionByIds(softwareBuildVersionIds));
    }

    /**
     * 删除关联的软件包
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param softwarePackageIds     软件包ID数组
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @Override
    @PostMapping(value = "/{softwareBuildVersionId}/action/removeSoftwarePackage/{softwarePackageIds}")
    public AjaxResult removeSoftwarePackage(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwarePackageIds) {
        logger.info("管理后台用户[{}]删除软件内部版本[{}]关联的软件包[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwarePackageIds);
        return toAjax(softwareBuildVersionAppService.deletePackage(softwareBuildVersionId, softwarePackageIds));
    }

    /**
     * 删除依赖的软件内部版本
     *
     * @param softwareBuildVersionId  软件内部版本ID
     * @param softwareBuildVersionIds 软件内部版本ID数组
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @Override
    @PostMapping(value = "/{softwareBuildVersionId}/action/removeDependency/{softwareBuildVersionIds}")
    public AjaxResult removeDependency(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwareBuildVersionIds) {
        logger.info("管理后台用户[{}]删除软件内部版本[{}]依赖的软件内部版本[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwareBuildVersionIds);
        return toAjax(softwareBuildVersionAppService.deleteDependency(softwareBuildVersionId, softwareBuildVersionIds));
    }

    /**
     * 删除配置字
     *
     * @param softwareBuildVersionId 软件内部版本ID
     * @param configWordIds          配置字ID数组
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @Override
    @PostMapping(value = "/{softwareBuildVersionId}/action/removeConfigWord/{configWordIds}")
    public AjaxResult removeConfigWord(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] configWordIds) {
        logger.info("管理后台用户[{}]删除软件内部版本[{}]配置字[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, configWordIds);
        return toAjax(softwareBuildVersionAppService.deleteConfigWord(softwareBuildVersionId, configWordIds));
    }
}
