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
import net.hwyz.iov.cloud.ota.pota.api.contract.CompatiblePnMpt;
import net.hwyz.iov.cloud.ota.pota.api.feign.mpt.CompatiblePnMptApi;
import net.hwyz.iov.cloud.ota.pota.service.application.service.CompatiblePnAppService;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.CompatiblePnMptAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.CompatiblePnPo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 兼容零件号相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/mpt/compatiblePn")
public class CompatiblePnMptController extends BaseController implements CompatiblePnMptApi {

    private final CompatiblePnAppService compatiblePnAppService;

    /**
     * 分页查询兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 兼容零件号列表
     */
    @RequiresPermissions("ota:baseline:compatiblePn:list")
    @Override
    @GetMapping(value = "/list")
    public TableDataInfo list(CompatiblePnMpt compatiblePn) {
        logger.info("管理后台用户[{}]分页查询兼容零件号", SecurityUtils.getUsername());
        startPage();
        List<CompatiblePnPo> compatiblePnPoList = compatiblePnAppService.search(compatiblePn.getDeviceCode(),
                compatiblePn.getType(), getBeginTime(compatiblePn), getEndTime(compatiblePn));
        List<CompatiblePnMpt> compatiblePnMptList = CompatiblePnMptAssembler.INSTANCE.fromPoList(compatiblePnPoList);
        return getDataTable(compatiblePnPoList, compatiblePnMptList);
    }

    /**
     * 导出兼容零件号
     *
     * @param response     响应
     * @param compatiblePn 兼容零件号
     */
    @Log(title = "兼容零件号管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:baseline:compatiblePn:export")
    @Override
    @PostMapping("/export")
    public void export(HttpServletResponse response, CompatiblePnMpt compatiblePn) {
        logger.info("管理后台用户[{}]导出兼容零件号", SecurityUtils.getUsername());
    }

    /**
     * 根据兼容零件号ID获取兼容零件号
     *
     * @param compatiblePnId 兼容零件号ID
     * @return 兼容零件号
     */
    @RequiresPermissions("ota:baseline:compatiblePn:query")
    @Override
    @GetMapping(value = "/{compatiblePnId}")
    public AjaxResult getInfo(@PathVariable Long compatiblePnId) {
        logger.info("管理后台用户[{}]根据兼容零件号ID[{}]获取兼容零件号", SecurityUtils.getUsername(), compatiblePnId);
        CompatiblePnPo compatiblePnPo = compatiblePnAppService.getCompatiblePnById(compatiblePnId);
        return success(CompatiblePnMptAssembler.INSTANCE.fromPo(compatiblePnPo));
    }

    /**
     * 新增兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 结果
     */
    @Log(title = "兼容零件号管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:baseline:compatiblePn:add")
    @Override
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CompatiblePnMpt compatiblePn) {
        logger.info("管理后台用户[{}]新增兼容零件号[{}]", SecurityUtils.getUsername(), compatiblePn.getDeviceCode());
        CompatiblePnPo compatiblePnPo = CompatiblePnMptAssembler.INSTANCE.toPo(compatiblePn);
        compatiblePnPo.setCreateBy(SecurityUtils.getUserId().toString());
        return toAjax(compatiblePnAppService.createCompatiblePn(compatiblePnPo));
    }

    /**
     * 修改保存兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 结果
     */
    @Log(title = "兼容零件号管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:baseline:compatiblePn:edit")
    @Override
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CompatiblePnMpt compatiblePn) {
        logger.info("管理后台用户[{}]修改保存兼容零件号[{}]", SecurityUtils.getUsername(), compatiblePn.getDeviceCode());
        CompatiblePnPo compatiblePnPo = CompatiblePnMptAssembler.INSTANCE.toPo(compatiblePn);
        compatiblePnPo.setModifyBy(SecurityUtils.getUserId().toString());
        return toAjax(compatiblePnAppService.modifyCompatiblePn(compatiblePnPo));
    }

    /**
     * 删除兼容零件号
     *
     * @param compatiblePnIds 兼容零件号ID数组
     * @return 结果
     */
    @Log(title = "兼容零件号管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:baseline:compatiblePn:remove")
    @Override
    @DeleteMapping("/{compatiblePnIds}")
    public AjaxResult remove(@PathVariable Long[] compatiblePnIds) {
        logger.info("管理后台用户[{}]删除兼容零件号[{}]", SecurityUtils.getUsername(), compatiblePnIds);
        return toAjax(compatiblePnAppService.deleteCompatiblePnByIds(compatiblePnIds));
    }
}
