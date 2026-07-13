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
import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwareBuildVersionMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwarePackageMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwareBuildVersionMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwarePackageMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.SoftwareBuildVersionDto;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwareBuildVersionAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwarePackageAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionTestReportPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwareBuildVersionAdaptationPo;
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
@RequestMapping(value = "/api/mpt/softwareBuildVersion/v1")
public class MptSoftwareBuildVersionController extends BaseController {

    private final MdmPartService mdmPartService;
    private final SoftwarePackageAppService softwarePackageAppService;
    private final SoftwareBuildVersionAppService softwareBuildVersionAppService;

    /**
     * 分页查询软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 软件内部版本信息列表
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<SoftwareBuildVersionMpt>> list(SoftwareBuildVersionMpt softwareBuildVersion) {
        log.info("管理后台用户[{}]分页查询软件内部版本信息", SecurityUtils.getUsername());
        startPage();
        List<SoftwareBuildVersionDto> dtoList = softwareBuildVersionAppService.searchDto(softwareBuildVersion.getDeviceCode(),
                softwareBuildVersion.getSoftwarePn(), null, getBeginTime(softwareBuildVersion), getEndTime(softwareBuildVersion));
        List<SoftwareBuildVersionMpt> softwareBuildVersionMptList = PageUtil.convert(dtoList, SoftwareBuildVersionMptAssembler.INSTANCE::fromDto);
        softwareBuildVersionMptList.forEach(softwareBuildVersionMpt -> {
            softwareBuildVersionMpt.setSoftwarePackageCount(softwareBuildVersionAppService.countPackage(softwareBuildVersionMpt.getId()));
            softwareBuildVersionMpt.setDependencyCount(softwareBuildVersionAppService.countDependency(softwareBuildVersionMpt.getId()));
            softwareBuildVersionMpt.setTestReportCount(softwareBuildVersionAppService.countTestReport(softwareBuildVersionMpt.getId()));
            softwareBuildVersionMpt.setAdaptationCount(softwareBuildVersionAppService.countAdaptation(softwareBuildVersionMpt.getId()));
            PartResponse part = mdmPartService.getByCode(softwareBuildVersionMpt.getSoftwarePn());
            if (ObjUtil.isNotNull(part)) {
                softwareBuildVersionMpt.setSoftwarePartName(part.getName());
            }
        });
        return ApiResponse.ok(getPageResult(softwareBuildVersionMptList));
    }

    /**
     * 查询软件内部版本下软件包
     *
     * @param softwareBuildVersionId 软件零件版本ID
     * @param softwarePackage        软件包
     * @return 软件包列表
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:list")
    @GetMapping(value = "/{softwareBuildVersionId}/listSoftwarePackage")
    public ApiResponse<List<SoftwarePackageMpt>> listSoftwarePackage(@PathVariable Long softwareBuildVersionId, SoftwarePackageMpt softwarePackage) {
        log.info("管理后台用户[{}]查询软件内部版本[{}]下软件包", SecurityUtils.getUsername(), softwareBuildVersionId);
        List<SoftwarePackagePo> softwarePackagePoList = softwarePackageAppService.search(softwarePackage.getDeviceCode(),
                softwarePackage.getSoftwarePn(), softwarePackage.getPackageCode(), softwarePackage.getPackageName(),
                softwareBuildVersionId, null, null);
        return ApiResponse.ok(SoftwarePackageMptAssembler.INSTANCE.fromPoList(softwarePackagePoList));
    }

    /**
     * 查询软件内部版本下依赖的软件内部版本
     *
     * @param softwareBuildVersionId 软件零件版本ID
     * @param softwareBuildVersion   软件零件版本信息
     * @return 依赖的软件零件版本列表
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:list")
    @GetMapping(value = "/{softwareBuildVersionId}/listDependency")
    public ApiResponse<List<SoftwareBuildVersionMpt>> listDependency(@PathVariable Long softwareBuildVersionId, SoftwareBuildVersionMpt softwareBuildVersion) {
        log.info("管理后台用户[{}]查询软件内部版本[{}]下依赖的软件零件版本", SecurityUtils.getUsername(), softwareBuildVersionId);
        List<SoftwareBuildVersionMpt> list = new ArrayList<>();
        softwareBuildVersionAppService.listDependency(softwareBuildVersionId).forEach(dependencyPo -> {
            SoftwareBuildVersionPo softwareBuildVersionPo = softwareBuildVersionAppService.getSoftwareBuildVersionById(dependencyPo.getDependencySoftwareBuildVersionId());
            SoftwareBuildVersionMpt softwareBuildVersionMpt = SoftwareBuildVersionMptAssembler.INSTANCE.fromPo(softwareBuildVersionPo);
            softwareBuildVersionMpt.setAdaptiveLevel(dependencyPo.getAdaptiveLevel());
            list.add(softwareBuildVersionMpt);
        });
        return ApiResponse.ok(list);
    }

    /**
     * 导出软件内部版本信息
     *
     * @param response             响应
     * @param softwareBuildVersion 软件内部版本信息
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:pota:softwareBuildVersion:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SoftwareBuildVersionMpt softwareBuildVersion) {
        log.info("管理后台用户[{}]导出软件零件版本信息", SecurityUtils.getUsername());
    }

    /**
     * 根据软件内部版本信息ID获取软件内部版本信息
     *
     * @param softwareBuildVersionId 软件内部版本信息ID
     * @return 软件内部版本信息
     */
    @RequiresPermissions("ota:pota:softwareBuildVersion:query")
    @GetMapping(value = "/{softwareBuildVersionId}")
    public ApiResponse<SoftwareBuildVersionMpt> getInfo(@PathVariable Long softwareBuildVersionId) {
        log.info("管理后台用户[{}]根据软件内部版本信息ID[{}]获取软件内部版本信息", SecurityUtils.getUsername(), softwareBuildVersionId);
        SoftwareBuildVersionPo softwareBuildVersionPo = softwareBuildVersionAppService.getSoftwareBuildVersionById(softwareBuildVersionId);
        return ApiResponse.ok(SoftwareBuildVersionMptAssembler.INSTANCE.fromPo(softwareBuildVersionPo));
    }

    /**
     * 新增软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:pota:softwareBuildVersion:add")
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody SoftwareBuildVersionMpt softwareBuildVersion) {
        log.info("管理后台用户[{}]新增零件[{}]软件内部版本信息[{}]", SecurityUtils.getUsername(), softwareBuildVersion.getSoftwarePn(), softwareBuildVersion.getSoftwareBuildVer());
        if (!softwareBuildVersionAppService.checkDeviceCodeAndSoftwarePnUnique(softwareBuildVersion.getId(), softwareBuildVersion.getDeviceCode(),
                softwareBuildVersion.getSoftwarePn(), softwareBuildVersion.getSoftwareBuildVer())) {
            return ApiResponse.fail("新增软件内部版本信息'" + softwareBuildVersion.getSoftwarePn() + "'失败，软件内部版本已存在");
        }
        SoftwareBuildVersionPo softwareBuildVersionPo = SoftwareBuildVersionMptAssembler.INSTANCE.toPo(softwareBuildVersion);
        softwareBuildVersionPo.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(softwareBuildVersionAppService.createSoftwareBuildVersion(softwareBuildVersionPo));
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
    @PostMapping(value = "/{softwareBuildVersionId}/action/addSoftwarePackage/{softwarePackageIds}")
    public ApiResponse<Integer> addSoftwarePackage(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwarePackageIds) {
        log.info("管理后台用户[{}]新增软件内部版本[{}]关联的软件包[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwarePackageIds);
        return ApiResponse.ok(softwareBuildVersionAppService.createPackage(softwareBuildVersionId, softwarePackageIds));
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
    @PostMapping(value = "/{softwareBuildVersionId}/action/addDependency/{softwareBuildVersionIds}")
    public ApiResponse<Integer> addDependency(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwareBuildVersionIds, @RequestParam Integer adaptiveLevel) {
        log.info("管理后台用户[{}]新增软件内部版本[{}]依赖的软件内部版本[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwareBuildVersionIds);
        return ApiResponse.ok(softwareBuildVersionAppService.createDependency(softwareBuildVersionId, softwareBuildVersionIds, adaptiveLevel));
    }

    /**
     * 修改保存软件内部版本信息
     *
     * @param softwareBuildVersion 软件内部版本信息
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody SoftwareBuildVersionMpt softwareBuildVersion) {
        log.info("管理后台用户[{}]修改保存零件[{}]软件内部版本信息[{}]", SecurityUtils.getUsername(), softwareBuildVersion.getSoftwarePn(), softwareBuildVersion.getSoftwareBuildVer());
        if (!softwareBuildVersionAppService.checkDeviceCodeAndSoftwarePnUnique(softwareBuildVersion.getId(),
                softwareBuildVersion.getDeviceCode(), softwareBuildVersion.getSoftwarePn(), softwareBuildVersion.getSoftwareBuildVer())) {
            return ApiResponse.fail("修改保存软件内部版本信息'" + softwareBuildVersion.getSoftwarePn() + "'失败，软件内部版本已存在");
        }
        SoftwareBuildVersionPo softwareBuildVersionPo = SoftwareBuildVersionMptAssembler.INSTANCE.toPo(softwareBuildVersion);
        softwareBuildVersionPo.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(softwareBuildVersionAppService.modifySoftwareBuildVersion(softwareBuildVersionPo));
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
    @PostMapping(value = "/{softwareBuildVersionId}/action/editDependency/{softwareBuildVersionIds}")
    public ApiResponse<Integer> editDependency(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwareBuildVersionIds, @RequestParam Integer adaptiveLevel) {
        log.info("管理后台用户[{}]修改保存软件内部版本[{}]依赖的软件内部版本[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwareBuildVersionIds);
        return ApiResponse.ok(softwareBuildVersionAppService.modifyDependency(softwareBuildVersionId, softwareBuildVersionIds, adaptiveLevel));
    }

    /**
     * 删除软件内部版本信息
     *
     * @param softwareBuildVersionIds 软件内部版本信息ID数组
     * @return 结果
     */
    @Log(title = "软件内部版本信息管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:remove")
    @DeleteMapping("/{softwareBuildVersionIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] softwareBuildVersionIds) {
        log.info("管理后台用户[{}]删除软件内部版本信息[{}]", SecurityUtils.getUsername(), softwareBuildVersionIds);
        return ApiResponse.ok(softwareBuildVersionAppService.deleteSoftwareBuildVersionByIds(softwareBuildVersionIds));
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
    @PostMapping(value = "/{softwareBuildVersionId}/action/removeSoftwarePackage/{softwarePackageIds}")
    public ApiResponse<Integer> removeSoftwarePackage(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwarePackageIds) {
        log.info("管理后台用户[{}]删除软件内部版本[{}]关联的软件包[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwarePackageIds);
        return ApiResponse.ok(softwareBuildVersionAppService.deletePackage(softwareBuildVersionId, softwarePackageIds));
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
    @PostMapping(value = "/{softwareBuildVersionId}/action/removeDependency/{softwareBuildVersionIds}")
    public ApiResponse<Integer> removeDependency(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] softwareBuildVersionIds) {
        log.info("管理后台用户[{}]删除软件内部版本[{}]依赖的软件内部版本[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, softwareBuildVersionIds);
        return ApiResponse.ok(softwareBuildVersionAppService.deleteDependency(softwareBuildVersionId, softwareBuildVersionIds));
    }

    // ==================== CR-004: 发布工作流状态流转 ====================

    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @PostMapping(value = "/{softwareBuildVersionId}/action/release")
    public ApiResponse<Integer> release(@PathVariable Long softwareBuildVersionId) {
        log.info("管理后台用户[{}]发布软件内部版本[{}]", SecurityUtils.getUsername(), softwareBuildVersionId);
        return ApiResponse.ok(softwareBuildVersionAppService.releaseSoftwareBuildVersion(softwareBuildVersionId));
    }

    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @PostMapping(value = "/{softwareBuildVersionId}/action/deprecate")
    public ApiResponse<Integer> deprecate(@PathVariable Long softwareBuildVersionId) {
        log.info("管理后台用户[{}]停用软件内部版本[{}]", SecurityUtils.getUsername(), softwareBuildVersionId);
        return ApiResponse.ok(softwareBuildVersionAppService.deprecateSoftwareBuildVersion(softwareBuildVersionId));
    }

    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @PostMapping(value = "/{softwareBuildVersionId}/action/retire")
    public ApiResponse<Integer> retire(@PathVariable Long softwareBuildVersionId) {
        log.info("管理后台用户[{}]退役软件内部版本[{}]", SecurityUtils.getUsername(), softwareBuildVersionId);
        return ApiResponse.ok(softwareBuildVersionAppService.retireSoftwareBuildVersion(softwareBuildVersionId));
    }

    // ==================== CR-004: 测试报告管理 ====================

    @RequiresPermissions("ota:pota:softwareBuildVersion:list")
    @GetMapping(value = "/{softwareBuildVersionId}/listTestReport")
    public ApiResponse<List<SoftwareBuildVersionTestReportPo>> listTestReport(@PathVariable Long softwareBuildVersionId) {
        log.info("管理后台用户[{}]查询软件内部版本[{}]测试报告", SecurityUtils.getUsername(), softwareBuildVersionId);
        return ApiResponse.ok(softwareBuildVersionAppService.listTestReports(softwareBuildVersionId));
    }

    @Log(title = "软件内部版本信息管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @PostMapping(value = "/{softwareBuildVersionId}/action/addTestReport")
    public ApiResponse<Integer> addTestReport(@PathVariable Long softwareBuildVersionId, @Validated @RequestBody SoftwareBuildVersionTestReportPo testReport) {
        log.info("管理后台用户[{}]新增软件内部版本[{}]测试报告", SecurityUtils.getUsername(), softwareBuildVersionId);
        testReport.setSbvId(softwareBuildVersionId);
        testReport.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(softwareBuildVersionAppService.createTestReport(testReport));
    }

    @Log(title = "软件内部版本信息管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @DeleteMapping(value = "/{softwareBuildVersionId}/testReport/{testReportIds}")
    public ApiResponse<Integer> removeTestReport(@PathVariable Long softwareBuildVersionId, @PathVariable Long[] testReportIds) {
        log.info("管理后台用户[{}]删除软件内部版本[{}]测试报告[{}]", SecurityUtils.getUsername(), softwareBuildVersionId, testReportIds);
        int result = 0;
        for (Long id : testReportIds) {
            result += softwareBuildVersionAppService.deleteTestReport(id);
        }
        return ApiResponse.ok(result);
    }

    // ==================== CR-004: 软硬件适配矩阵管理 ====================

    @RequiresPermissions("ota:pota:softwareBuildVersion:list")
    @GetMapping(value = "/{softwareBuildVersionId}/listAdaptation")
    public ApiResponse<List<SoftwareBuildVersionAdaptationPo>> listAdaptation(@PathVariable Long softwareBuildVersionId) {
        log.info("管理后台用户[{}]查询软件内部版本[{}]适配矩阵", SecurityUtils.getUsername(), softwareBuildVersionId);
        return ApiResponse.ok(softwareBuildVersionAppService.listAdaptations(softwareBuildVersionId));
    }

    @Log(title = "软件内部版本信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwareBuildVersion:edit")
    @PostMapping(value = "/{softwareBuildVersionId}/action/saveAdaptation")
    public ApiResponse<Integer> saveAdaptation(@PathVariable Long softwareBuildVersionId, @Validated @RequestBody List<SoftwareBuildVersionAdaptationPo> adaptations) {
        log.info("管理后台用户[{}]保存软件内部版本[{}]适配矩阵", SecurityUtils.getUsername(), softwareBuildVersionId);
        adaptations.forEach(a -> a.setCreateBy(SecurityUtils.getUserId().toString()));
        return ApiResponse.ok(softwareBuildVersionAppService.saveAdaptations(softwareBuildVersionId, adaptations));
    }

}
