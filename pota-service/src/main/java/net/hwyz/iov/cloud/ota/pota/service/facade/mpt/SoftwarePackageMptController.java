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
import net.hwyz.iov.cloud.ota.pota.api.contract.SoftwarePackageMpt;
import net.hwyz.iov.cloud.ota.pota.api.feign.mpt.SoftwarePackageMptApi;
import net.hwyz.iov.cloud.ota.pota.service.application.service.SoftwarePackageAppService;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.SoftwarePackageMptAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.SoftwarePackagePo;
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
@RequestMapping(value = "/mpt/softwarePackage")
public class SoftwarePackageMptController extends BaseController implements SoftwarePackageMptApi {

    private final SoftwarePackageAppService softwarePackageAppService;

    /**
     * 分页查询软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 软件包信息列表
     */
    @RequiresPermissions("ota:pota:softwarePackage:list")
    @Override
    @GetMapping(value = "/list")
    public TableDataInfo list(SoftwarePackageMpt softwarePackage) {
        logger.info("管理后台用户[{}]分页查询软件包信息", SecurityUtils.getUsername());
        startPage();
        List<SoftwarePackagePo> softwarePackagePoList = softwarePackageAppService.search(softwarePackage.getDeviceCode(),
                softwarePackage.getSoftwarePn(), softwarePackage.getPackageCode(), softwarePackage.getPackageName(),
                null, getBeginTime(softwarePackage), getEndTime(softwarePackage));
        List<SoftwarePackageMpt> softwarePackageMptList = SoftwarePackageMptAssembler.INSTANCE.fromPoList(softwarePackagePoList);
        return getDataTable(softwarePackagePoList, softwarePackageMptList);
    }

    /**
     * 导出软件包信息
     *
     * @param response        响应
     * @param softwarePackage 软件包信息
     */
    @Log(title = "软件包信息管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:pota:softwarePackage:export")
    @Override
    @PostMapping("/export")
    public void export(HttpServletResponse response, SoftwarePackageMpt softwarePackage) {
        logger.info("管理后台用户[{}]导出软件包信息", SecurityUtils.getUsername());
    }

    /**
     * 根据软件包信息ID获取软件包信息
     *
     * @param softwarePackageId 软件包信息ID
     * @return 软件包信息
     */
    @RequiresPermissions("ota:pota:softwarePackage:query")
    @Override
    @GetMapping(value = "/{softwarePackageId}")
    public AjaxResult getInfo(@PathVariable Long softwarePackageId) {
        logger.info("管理后台用户[{}]根据软件包信息ID[{}]获取软件包信息", SecurityUtils.getUsername(), softwarePackageId);
        SoftwarePackagePo softwarePackagePo = softwarePackageAppService.getSoftwarePackageById(softwarePackageId);
        return success(SoftwarePackageMptAssembler.INSTANCE.fromPo(softwarePackagePo));
    }

    /**
     * 新增软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    @Log(title = "软件包信息管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:pota:softwarePackage:add")
    @Override
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SoftwarePackageMpt softwarePackage) {
        logger.info("管理后台用户[{}]新增软件包信息[{}]", SecurityUtils.getUsername(), softwarePackage.getPackageCode());
        SoftwarePackagePo softwarePackagePo = SoftwarePackageMptAssembler.INSTANCE.toPo(softwarePackage);
        softwarePackagePo.setCreateBy(SecurityUtils.getUserId().toString());
        return toAjax(softwarePackageAppService.createSoftwarePackage(softwarePackagePo));
    }

    /**
     * 修改保存软件包信息
     *
     * @param softwarePackage 软件包信息
     * @return 结果
     */
    @Log(title = "软件包信息管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:pota:softwarePackage:edit")
    @Override
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SoftwarePackageMpt softwarePackage) {
        logger.info("管理后台用户[{}]修改保存软件包信息[{}]", SecurityUtils.getUsername(), softwarePackage.getPackageCode());
        SoftwarePackagePo softwarePackagePo = SoftwarePackageMptAssembler.INSTANCE.toPo(softwarePackage);
        softwarePackagePo.setModifyBy(SecurityUtils.getUserId().toString());
        return toAjax(softwarePackageAppService.modifySoftwarePackage(softwarePackagePo));
    }

    /**
     * 删除软件包信息
     *
     * @param softwarePackageIds 软件包信息ID数组
     * @return 结果
     */
    @Log(title = "软件包信息管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:pota:softwarePartVersion:remove")
    @Override
    @DeleteMapping("/{softwarePackageIds}")
    public AjaxResult remove(@PathVariable Long[] softwarePackageIds) {
        logger.info("管理后台用户[{}]删除软件包信息[{}]", SecurityUtils.getUsername(), softwarePackageIds);
        return toAjax(softwarePackageAppService.deleteSoftwarePackageByIds(softwarePackageIds));
    }

}
