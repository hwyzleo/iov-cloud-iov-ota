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
import net.hwyz.iov.cloud.ota.pota.api.contract.FixedConfigWordMpt;
import net.hwyz.iov.cloud.ota.pota.api.contract.enums.ConfigWordType;
import net.hwyz.iov.cloud.ota.pota.api.feign.mpt.FixedConfigWordMptApi;
import net.hwyz.iov.cloud.ota.pota.service.application.service.FixedConfigWordAppService;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.ConfigWordMptAssembler;
import net.hwyz.iov.cloud.ota.pota.service.facade.assembler.FixedConfigWordMptAssembler;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.ConfigWordPo;
import net.hwyz.iov.cloud.ota.pota.service.infrastructure.repository.po.FixedConfigWordPo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 固定配置字相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/mpt/fixedConfigWord")
public class FixedConfigWordMptController extends BaseController implements FixedConfigWordMptApi {

    private final FixedConfigWordAppService fixedConfigWordAppService;

    /**
     * 分页查询固定配置字
     *
     * @param fixedConfigWord 固定配置字
     * @return 固定配置字列表
     */
    @RequiresPermissions("ota:baseline:fixedConfigWord:list")
    @Override
    @GetMapping(value = "/list")
    public TableDataInfo list(FixedConfigWordMpt fixedConfigWord) {
        logger.info("管理后台用户[{}]分页查询固定配置字", SecurityUtils.getUsername());
        startPage();
        List<FixedConfigWordPo> fixedConfigWordPoList = fixedConfigWordAppService.search(fixedConfigWord.getDeviceCode(),
                fixedConfigWord.getType(), null, getBeginTime(fixedConfigWord), getEndTime(fixedConfigWord));
        List<FixedConfigWordMpt> fixedConfigWordMptList = FixedConfigWordMptAssembler.INSTANCE.fromPoList(fixedConfigWordPoList);
        return getDataTable(fixedConfigWordPoList, fixedConfigWordMptList);
    }

    /**
     * 列出配置字列表
     *
     * @param fixedConfigWordId 固定配置字ID
     * @return 配置字列表
     */
    @RequiresPermissions("ota:baseline:fixedConfigWord:query")
    @Override
    @GetMapping(value = "/{fixedConfigWordId}/configWord")
    public AjaxResult listConfigWord(@PathVariable Long fixedConfigWordId) {
        logger.info("管理后台用户[{}]列出固定配置字[{}]下配置字列表", SecurityUtils.getUsername(), fixedConfigWordId);
        List<ConfigWordPo> configWordPoList = fixedConfigWordAppService.listConfigWordByFixedConfigWordId(fixedConfigWordId);
        return success(ConfigWordMptAssembler.INSTANCE.fromPoList(configWordPoList));
    }

    /**
     * 导出固定配置字
     *
     * @param response        响应
     * @param fixedConfigWord 固定配置字
     */
    @Log(title = "固定配置字管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:baseline:fixedConfigWord:export")
    @Override
    @PostMapping("/export")
    public void export(HttpServletResponse response, FixedConfigWordMpt fixedConfigWord) {
        logger.info("管理后台用户[{}]导出固定配置字", SecurityUtils.getUsername());
    }

    /**
     * 根据固定配置字ID获取固定配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @return 固定配置字
     */
    @RequiresPermissions("ota:baseline:fixedConfigWord:query")
    @Override
    @GetMapping(value = "/{fixedConfigWordId}")
    public AjaxResult getInfo(@PathVariable Long fixedConfigWordId) {
        logger.info("管理后台用户[{}]根据固定配置字ID[{}]获取固定配置字", SecurityUtils.getUsername(), fixedConfigWordId);
        FixedConfigWordPo fixedConfigWordPo = fixedConfigWordAppService.getFixedConfigWordById(fixedConfigWordId);
        return success(FixedConfigWordMptAssembler.INSTANCE.fromPo(fixedConfigWordPo));
    }

    /**
     * 根据配置字ID获取配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @param configWordId      配置字ID
     * @return 配置字
     */
    @RequiresPermissions("ota:baseline:fixedConfigWord:query")
    @Override
    @GetMapping(value = "/{fixedConfigWordId}/configWord/{configWordId}")
    public AjaxResult getConfigWordInfo(@PathVariable Long fixedConfigWordId, @PathVariable Long configWordId) {
        logger.info("管理后台用户[{}]根据配置字ID[{}]获取配置字", SecurityUtils.getUsername(), configWordId);
        ConfigWordPo configWordPo = fixedConfigWordAppService.getConfigWordById(configWordId);
        return success(ConfigWordMptAssembler.INSTANCE.fromPo(configWordPo));
    }

    /**
     * 新增固定配置字
     *
     * @param fixedConfigWord 固定配置字
     * @return 结果
     */
    @Log(title = "固定配置字管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:baseline:fixedConfigWord:add")
    @Override
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FixedConfigWordMpt fixedConfigWord) {
        logger.info("管理后台用户[{}]新增固定配置字[{}]", SecurityUtils.getUsername(), fixedConfigWord.getDeviceCode());
        FixedConfigWordPo fixedConfigWordPo = FixedConfigWordMptAssembler.INSTANCE.toPo(fixedConfigWord);
        fixedConfigWordPo.setCreateBy(SecurityUtils.getUserId().toString());
        return toAjax(fixedConfigWordAppService.createFixedConfigWord(fixedConfigWordPo));
    }

    /**
     * 新增配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @param configWord        配置字
     * @return 结果
     */
    @Log(title = "固定配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:baseline:fixedConfigWord:edit")
    @Override
    @PostMapping("/{fixedConfigWordId}/configWord")
    public AjaxResult addConfigWord(@PathVariable Long fixedConfigWordId, @Validated @RequestBody ConfigWordMpt configWord) {
        logger.info("管理后台用户[{}]新增固定配置字[{}]配置字", SecurityUtils.getUsername(), fixedConfigWordId);
        ConfigWordPo configWordPo = ConfigWordMptAssembler.INSTANCE.toPo(configWord);
        configWordPo.setType(ConfigWordType.FIXED.value);
        configWordPo.setReferenceId(fixedConfigWordId);
        configWordPo.setCreateBy(SecurityUtils.getUserId().toString());
        return toAjax(fixedConfigWordAppService.createConfigWord(configWordPo));
    }

    /**
     * 修改保存固定配置字
     *
     * @param fixedConfigWord 固定配置字
     * @return 结果
     */
    @Log(title = "固定配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:baseline:fixedConfigWord:edit")
    @Override
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FixedConfigWordMpt fixedConfigWord) {
        logger.info("管理后台用户[{}]修改保存固定配置字[{}]", SecurityUtils.getUsername(), fixedConfigWord.getDeviceCode());
        FixedConfigWordPo fixedConfigWordPo = FixedConfigWordMptAssembler.INSTANCE.toPo(fixedConfigWord);
        fixedConfigWordPo.setModifyBy(SecurityUtils.getUserId().toString());
        return toAjax(fixedConfigWordAppService.modifyFixedConfigWord(fixedConfigWordPo));
    }

    /**
     * 修改保存配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @param configWord        配置字
     * @return 结果
     */
    @Log(title = "固定配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:baseline:fixedConfigWord:edit")
    @Override
    @PutMapping("/{fixedConfigWordId}/configWord")
    public AjaxResult editConfigWord(@PathVariable Long fixedConfigWordId, @Validated @RequestBody ConfigWordMpt configWord) {
        logger.info("管理后台用户[{}]修改保存固定配置字[{}]配置字", SecurityUtils.getUsername(), fixedConfigWordId);
        ConfigWordPo configWordPo = ConfigWordMptAssembler.INSTANCE.toPo(configWord);
        configWordPo.setModifyBy(SecurityUtils.getUserId().toString());
        return toAjax(fixedConfigWordAppService.modifyConfigWord(configWordPo));
    }

    /**
     * 删除固定配置字
     *
     * @param fixedConfigWordIds 固定配置字ID数组
     * @return 结果
     */
    @Log(title = "固定配置字管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:baseline:fixedConfigWord:remove")
    @Override
    @DeleteMapping("/{fixedConfigWordIds}")
    public AjaxResult remove(@PathVariable Long[] fixedConfigWordIds) {
        logger.info("管理后台用户[{}]删除固定配置字[{}]", SecurityUtils.getUsername(), fixedConfigWordIds);
        return toAjax(fixedConfigWordAppService.deleteFixedConfigWordByIds(fixedConfigWordIds));
    }

    /**
     * 删除配置字
     *
     * @param fixedConfigWordId 固定配置字ID
     * @param configWordIds     配置字ID数组
     * @return 结果
     */
    @Log(title = "固定配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:baseline:fixedConfigWord:edit")
    @Override
    @DeleteMapping("/{fixedConfigWordId}/configWord/{configWordIds}")
    public AjaxResult removeConfigWord(@PathVariable Long fixedConfigWordId, @PathVariable Long[] configWordIds) {
        logger.info("管理后台用户[{}]删除固定配置字[{}]配置字[{}]", SecurityUtils.getUsername(), fixedConfigWordId, configWordIds);
        return toAjax(fixedConfigWordAppService.deleteConfigWordByIds(configWordIds));
    }
}
