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
import net.hwyz.iov.cloud.iov.ota.api.vo.CompatiblePnMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.CompatiblePnMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.CompatiblePnAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.CompatiblePnPo;
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
@RequestMapping(value = "/api/mpt/compatiblePn/v1")
public class MptCompatiblePnController extends BaseController {

    private final CompatiblePnAppService compatiblePnAppService;

    /**
     * 分页查询兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 兼容零件号列表
     */
    @RequiresPermissions("ota:baseline:compatiblePn:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<CompatiblePnMpt>> list(CompatiblePnMpt compatiblePn) {
        log.info("管理后台用户[{}]分页查询兼容零件号", SecurityUtils.getUsername());
        startPage();
        List<CompatiblePnPo> compatiblePnPoList = compatiblePnAppService.search(compatiblePn.getDeviceCode(),
                compatiblePn.getType(), getBeginTime(compatiblePn), getEndTime(compatiblePn));
        return ApiResponse.ok(getPageResult(PageUtil.convert(compatiblePnPoList, CompatiblePnMptAssembler.INSTANCE::fromPo)));
    }

    /**
     * 导出兼容零件号
     *
     * @param response     响应
     * @param compatiblePn 兼容零件号
     */
    @Log(title = "兼容零件号管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:baseline:compatiblePn:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CompatiblePnMpt compatiblePn) {
        log.info("管理后台用户[{}]导出兼容零件号", SecurityUtils.getUsername());
    }

    /**
     * 根据兼容零件号ID获取兼容零件号
     *
     * @param compatiblePnId 兼容零件号ID
     * @return 兼容零件号
     */
    @RequiresPermissions("ota:baseline:compatiblePn:query")
    @GetMapping(value = "/{compatiblePnId}")
    public ApiResponse<CompatiblePnMpt> getInfo(@PathVariable Long compatiblePnId) {
        log.info("管理后台用户[{}]根据兼容零件号ID[{}]获取兼容零件号", SecurityUtils.getUsername(), compatiblePnId);
        CompatiblePnPo compatiblePnPo = compatiblePnAppService.getCompatiblePnById(compatiblePnId);
        return ApiResponse.ok(CompatiblePnMptAssembler.INSTANCE.fromPo(compatiblePnPo));
    }

    /**
     * 新增兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 结果
     */
    @Log(title = "兼容零件号管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:baseline:compatiblePn:add")
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody CompatiblePnMpt compatiblePn) {
        log.info("管理后台用户[{}]新增兼容零件号[{}]", SecurityUtils.getUsername(), compatiblePn.getDeviceCode());
        CompatiblePnPo compatiblePnPo = CompatiblePnMptAssembler.INSTANCE.toPo(compatiblePn);
        compatiblePnPo.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(compatiblePnAppService.createCompatiblePn(compatiblePnPo));
    }

    /**
     * 修改保存兼容零件号
     *
     * @param compatiblePn 兼容零件号
     * @return 结果
     */
    @Log(title = "兼容零件号管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:baseline:compatiblePn:edit")
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody CompatiblePnMpt compatiblePn) {
        log.info("管理后台用户[{}]修改保存兼容零件号[{}]", SecurityUtils.getUsername(), compatiblePn.getDeviceCode());
        CompatiblePnPo compatiblePnPo = CompatiblePnMptAssembler.INSTANCE.toPo(compatiblePn);
        compatiblePnPo.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(compatiblePnAppService.modifyCompatiblePn(compatiblePnPo));
    }

    /**
     * 删除兼容零件号
     *
     * @param compatiblePnIds 兼容零件号ID数组
     * @return 结果
     */
    @Log(title = "兼容零件号管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:baseline:compatiblePn:remove")
    @DeleteMapping("/{compatiblePnIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] compatiblePnIds) {
        log.info("管理后台用户[{}]删除兼容零件号[{}]", SecurityUtils.getUsername(), compatiblePnIds);
        return ApiResponse.ok(compatiblePnAppService.deleteCompatiblePnByIds(compatiblePnIds));
    }
}
