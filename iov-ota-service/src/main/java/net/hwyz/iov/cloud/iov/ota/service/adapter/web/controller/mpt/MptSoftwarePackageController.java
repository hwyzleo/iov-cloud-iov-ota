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
import net.hwyz.iov.cloud.iov.ota.api.vo.SoftwarePackageMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.SoftwarePackageMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.SoftwarePackageAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.SoftwarePackagePo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 软件包信息相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/softwarePackage/v1")
public class MptSoftwarePackageController extends BaseController {

    private final SoftwarePackageAppService softwarePackageAppService;

    /**
     * 分页查询软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 软件包信息列表
     */
    @RequiresPermissions("ota:pota:softwarePackage:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<SoftwarePackageMpt>> list(SoftwarePackageMpt softwarePackage) {
        log.info("管理后台用户[{}]分页查询软件包信息", SecurityUtils.getUsername());
        startPage();
        List<SoftwarePackagePo> softwarePackagePoList = softwarePackageAppService.search(softwarePackage.getDeviceCode(),
                softwarePackage.getSoftwarePn(), softwarePackage.getPackageCode(), softwarePackage.getPackageName(),
                null, getBeginTime(softwarePackage), getEndTime(softwarePackage));
        return ApiResponse.ok(getPageResult(PageUtil.convert(softwarePackagePoList, SoftwarePackageMptAssembler.INSTANCE::fromPo)));
    }

    /**
     * 导出软件包信息
     *
     * @param response        响应
     * @param softwarePackage 软件包信息
     */
    @Log(title = "软件包信息管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:pota:softwarePackage:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SoftwarePackageMpt softwarePackage) {
        log.info("管理后台用户[{}]导出软件包信息", SecurityUtils.getUsername());
    }

    /**
     * 根据软件包信息ID获取软件包信息
     *
     * @param softwarePackageId 软件包信息ID
     * @return 软件包信息
     */
    @RequiresPermissions("ota:pota:softwarePackage:query")
    @GetMapping(value = "/{softwarePackageId}")
    public ApiResponse<SoftwarePackageMpt> getInfo(@PathVariable Long softwarePackageId) {
        log.info("管理后台用户[{}]根据软件包信息ID[{}]获取软件包信息", SecurityUtils.getUsername(), softwarePackageId);
        SoftwarePackagePo softwarePackagePo = softwarePackageAppService.getSoftwarePackageById(softwarePackageId);
        return ApiResponse.ok(SoftwarePackageMptAssembler.INSTANCE.fromPo(softwarePackagePo));
    }

    /**
     * 新增软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    @Log(title = "软件包信息管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:pota:softwarePackage:add")
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody SoftwarePackageMpt softwarePackage) {
        log.info("管理后台用户[{}]新增软件包信息[{}]", SecurityUtils.getUsername(), softwarePackage.getPackageCode());
        SoftwarePackagePo softwarePackagePo = SoftwarePackageMptAssembler.INSTANCE.toPo(softwarePackage);
        softwarePackagePo.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(softwarePackageAppService.createSoftwarePackage(softwarePackagePo));
    }

    /**
     * 修改保存软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    @Log(title = "软件包信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwarePackage:edit")
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody SoftwarePackageMpt softwarePackage) {
        log.info("管理后台用户[{}]修改保存软件包信息[{}]", SecurityUtils.getUsername(), softwarePackage.getPackageCode());
        SoftwarePackagePo softwarePackagePo = SoftwarePackageMptAssembler.INSTANCE.toPo(softwarePackage);
        softwarePackagePo.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(softwarePackageAppService.modifySoftwarePackage(softwarePackagePo));
    }

    /**
     * 删除软件包信息
     *
     * @param softwarePackageIds 软件包信息ID数组
     * @return 结果
     */
    @Log(title = "软件包信息管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:pota:softwarePartVersion:remove")
    @DeleteMapping("/{softwarePackageIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] softwarePackageIds) {
        log.info("管理后台用户[{}]删除软件包信息[{}]", SecurityUtils.getUsername(), softwarePackageIds);
        return ApiResponse.ok(softwarePackageAppService.deleteSoftwarePackageByIds(softwarePackageIds));
    }

    // ==================== CR-004: 制品可用性状态流转 ====================

    @Log(title = "软件包信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwarePackage:edit")
    @PostMapping(value = "/{softwarePackageId}/action/deprecate")
    public ApiResponse<Integer> deprecate(@PathVariable Long softwarePackageId) {
        log.info("管理后台用户[{}]停用软件包[{}]", SecurityUtils.getUsername(), softwarePackageId);
        return ApiResponse.ok(softwarePackageAppService.deprecateSoftwarePackage(softwarePackageId));
    }

    @Log(title = "软件包信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwarePackage:edit")
    @PostMapping(value = "/{softwarePackageId}/action/revoke")
    public ApiResponse<Integer> revoke(@PathVariable Long softwarePackageId) {
        log.info("管理后台用户[{}]吊销软件包[{}]", SecurityUtils.getUsername(), softwarePackageId);
        return ApiResponse.ok(softwarePackageAppService.revokeSoftwarePackage(softwarePackageId));
    }

    @Log(title = "软件包信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwarePackage:edit")
    @PostMapping(value = "/{softwarePackageId}/action/retire")
    public ApiResponse<Integer> retire(@PathVariable Long softwarePackageId) {
        log.info("管理后台用户[{}]退役软件包[{}]", SecurityUtils.getUsername(), softwarePackageId);
        return ApiResponse.ok(softwarePackageAppService.retireSoftwarePackage(softwarePackageId));
    }

}
