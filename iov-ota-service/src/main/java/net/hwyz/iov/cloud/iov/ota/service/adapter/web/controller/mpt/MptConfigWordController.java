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
import net.hwyz.iov.cloud.iov.ota.api.vo.ConfigWordFieldMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.ConfigWordMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.ConfigWordProfileMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ConfigWordFieldMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ConfigWordMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.ConfigWordProfileMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ConfigWordAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordFieldPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ConfigWordProfilePo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 配置字相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/configWord/v1")
public class MptConfigWordController extends BaseController {

    private final ConfigWordAppService configWordAppService;

    /**
     * 分页查询配置字
     *
     * @param configWord 配置字
     * @return 配置字列表
     */
    @RequiresPermissions("ota:dota:configWord:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<ConfigWordMpt>> list(ConfigWordMpt configWord) {
        log.info("管理后台用户[{}]分页查询配置字", SecurityUtils.getUsername());
        startPage();
        List<ConfigWordPo> configWordPoList = configWordAppService.search(configWord.getDeviceCode(), configWord.getCode(),
                configWord.getName(), getBeginTime(configWord), getEndTime(configWord));
        return ApiResponse.ok(getPageResult(PageUtil.convert(configWordPoList, ConfigWordMptAssembler.INSTANCE::fromPo)));
    }

    /**
     * 查询固定配置字列表
     *
     * @param configWord 配置字
     * @return 固定配置字列表
     */
    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/listFixedConfigWord")
    public ApiResponse<List<ConfigWordMpt>> listFixedConfigWord(ConfigWordMpt configWord) {
        log.info("管理后台用户[{}]查询固定配置字列表", SecurityUtils.getUsername());
        List<ConfigWordPo> configWordPoList = configWordAppService.search(configWord.getDeviceCode(), configWord.getCode(),
                configWord.getName(), null, null);
        return ApiResponse.ok(PageUtil.convert(configWordPoList, ConfigWordMptAssembler.INSTANCE::fromPo));
    }

    /**
     * 分页查询配置字配置文件
     *
     * @param configWordCode    配置字代码
     * @param configWordProfile 配置字配置文件
     * @return 配置字配置文件列表
     */
    @RequiresPermissions("ota:dota:configWord:list")
    @GetMapping(value = "/{configWordCode}/profile/list")
    public ApiResponse<PageResult<ConfigWordProfileMpt>> listProfile(@PathVariable String configWordCode, ConfigWordProfileMpt configWordProfile) {
        log.info("管理后台用户[{}]分页查询配置字[{}]配置文件", SecurityUtils.getUsername(), configWordCode);
        startPage();
        List<ConfigWordProfilePo> configWordProfilePoList = configWordAppService.searchProfile(configWordProfile.getConfigWordCode(),
                configWordProfile.getCode(), configWordProfile.getName(), getBeginTime(configWordProfile), getEndTime(configWordProfile));
        return ApiResponse.ok(getPageResult(PageUtil.convert(configWordProfilePoList, ConfigWordProfileMptAssembler.INSTANCE::fromPo)));
    }

    /**
     * 分页查询配置字字段
     *
     * @param configWordCode  配置字代码
     * @param configWordField 配置字字段
     * @return 配置字字段列表
     */
    @RequiresPermissions("ota:dota:configWord:list")
    @GetMapping(value = "/{configWordCode}/profile/{configWordProfileCode}/field/list")
    public ApiResponse<PageResult<ConfigWordFieldMpt>> listField(@PathVariable String configWordCode, @PathVariable String configWordProfileCode,
                                                                  ConfigWordFieldMpt configWordField) {
        log.info("管理后台用户[{}]分页查询配置字[{}]下配置文件[{}]字段", SecurityUtils.getUsername(), configWordCode, configWordProfileCode);
        startPage();
        List<ConfigWordFieldPo> configWordFieldPoList = configWordAppService.searchField(configWordCode, configWordProfileCode,
                configWordField.getCode(), configWordField.getName(), getBeginTime(configWordField), getEndTime(configWordField));
        return ApiResponse.ok(getPageResult(PageUtil.convert(configWordFieldPoList, ConfigWordFieldMptAssembler.INSTANCE::fromPo)));
    }

    /**
     * 导出配置字
     *
     * @param response   响应
     * @param configWord 配置字
     */
    @Log(title = "配置字管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:dota:configWord:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ConfigWordMpt configWord) {
        log.info("管理后台用户[{}]导出配置字", SecurityUtils.getUsername());
    }

    /**
     * 根据配置字ID获取配置字
     *
     * @param configWordId 配置字ID
     * @return 配置字
     */
    @RequiresPermissions("ota:dota:configWord:query")
    @GetMapping(value = "/{configWordId}")
    public ApiResponse<ConfigWordMpt> getInfo(@PathVariable Long configWordId) {
        log.info("管理后台用户[{}]根据配置字ID[{}]获取配置字", SecurityUtils.getUsername(), configWordId);
        ConfigWordPo configWordPo = configWordAppService.getConfigWordById(configWordId);
        return ApiResponse.ok(ConfigWordMptAssembler.INSTANCE.fromPo(configWordPo));
    }

    /**
     * 根据配置字配置文件ID获取配置字配置文件
     *
     * @param configWordCode      配置字代码
     * @param configWordProfileId 配置字配置文件ID
     * @return 配置字配置文件
     */
    @RequiresPermissions("ota:dota:configWord:query")
    @GetMapping(value = "/{configWordCode}/profile/{configWordProfileId}")
    public ApiResponse<ConfigWordProfileMpt> getProfileInfo(@PathVariable String configWordCode, @PathVariable Long configWordProfileId) {
        log.info("管理后台用户[{}]根据配置字[{}]配置文件ID[{}]获取配置字配置文件", SecurityUtils.getUsername(), configWordCode, configWordProfileId);
        ConfigWordProfilePo configWordProfilePo = configWordAppService.getConfigWordProfileById(configWordCode, configWordProfileId);
        return ApiResponse.ok(ConfigWordProfileMptAssembler.INSTANCE.fromPo(configWordProfilePo));
    }

    /**
     * 根据配置字字段ID获取配置字字段
     *
     * @param configWordCode    配置字代码
     * @param configWordFieldId 配置字字段ID
     * @return 配置字字段
     */
    @RequiresPermissions("ota:dota:configWord:query")
    @GetMapping(value = "/{configWordCode}/profile/{configWordProfileCode}/field/{configWordFieldId}")
    public ApiResponse<ConfigWordFieldMpt> getFieldInfo(@PathVariable String configWordCode, @PathVariable String configWordProfileCode, @PathVariable Long configWordFieldId) {
        log.info("管理后台用户[{}]根据配置字[{}]配置文件[{}]字段ID[{}]获取配置字字段", SecurityUtils.getUsername(), configWordCode, configWordProfileCode, configWordFieldId);
        ConfigWordFieldPo configWordFieldPo = configWordAppService.getConfigWordFieldById(configWordCode, configWordProfileCode, configWordFieldId);
        return ApiResponse.ok(ConfigWordFieldMptAssembler.INSTANCE.fromPo(configWordFieldPo));
    }

    /**
     * 新增配置字
     *
     * @param configWord 配置字
     * @return 结果
     */
    @Log(title = "配置字管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:dota:configWord:add")
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody ConfigWordMpt configWord) {
        log.info("管理后台用户[{}]新增配置字[{}]", SecurityUtils.getUsername(), configWord.getCode());
        if (!configWordAppService.checkCodeUnique(configWord.getId(), configWord.getCode())) {
            return ApiResponse.fail("新增配置字'" + configWord.getCode() + "'失败，配置字代码已存在");
        }
        ConfigWordPo configWordPo = ConfigWordMptAssembler.INSTANCE.toPo(configWord);
        configWordPo.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(configWordAppService.createConfigWord(configWordPo));
    }

    /**
     * 新增配置字配置文件
     *
     * @param configWordCode    配置字代码
     * @param configWordProfile 配置字配置文件
     * @return 结果
     */
    @Log(title = "配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:dota:configWord:edit")
    @PostMapping("/{configWordCode}/profile")
    public ApiResponse<Integer> addProfile(@PathVariable String configWordCode, @Validated @RequestBody ConfigWordProfileMpt configWordProfile) {
        log.info("管理后台用户[{}]新增配置字[{}]配置文件[{}]", SecurityUtils.getUsername(), configWordCode, configWordProfile.getCode());
        if (!configWordAppService.checkProfileCodeUnique(configWordProfile.getId(), configWordCode, configWordProfile.getCode())) {
            return ApiResponse.fail("新增配置字配置文件'" + configWordProfile.getCode() + "'失败，配置字配置文件代码已存在");
        }
        ConfigWordProfilePo configWordProfilePo = ConfigWordProfileMptAssembler.INSTANCE.toPo(configWordProfile);
        configWordProfilePo.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(configWordAppService.createConfigWordProfile(configWordCode, configWordProfilePo));
    }

    /**
     * 新增配置字字段
     *
     * @param configWordCode  配置字代码
     * @param configWordField 配置字字段
     * @return 结果
     */
    @Log(title = "配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:dota:configWord:edit")
    @PostMapping("/{configWordCode}/profile/{configWordProfileCode}/field")
    public ApiResponse<Integer> addField(@PathVariable String configWordCode, @PathVariable String configWordProfileCode,
                                         @Validated @RequestBody ConfigWordFieldMpt configWordField) {
        log.info("管理后台用户[{}]新增配置字[{}]配置文件[{}]字段[{}]", SecurityUtils.getUsername(), configWordCode,
                configWordProfileCode, configWordField.getCode());
        if (!configWordAppService.checkFieldCodeUnique(configWordField.getId(), configWordCode, configWordProfileCode,
                configWordField.getCode())) {
            return ApiResponse.fail("新增配置字字段'" + configWordField.getCode() + "'失败，配置字字段代码已存在");
        }
        ConfigWordFieldPo configWordFieldPo = ConfigWordFieldMptAssembler.INSTANCE.toPo(configWordField);
        configWordFieldPo.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(configWordAppService.createConfigWordField(configWordCode, configWordFieldPo));
    }

    /**
     * 修改保存配置字
     *
     * @param configWord 配置字
     * @return 结果
     */
    @Log(title = "配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:dota:configWord:edit")
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody ConfigWordMpt configWord) {
        log.info("管理后台用户[{}]修改保存配置字[{}]", SecurityUtils.getUsername(), configWord.getCode());
        if (!configWordAppService.checkCodeUnique(configWord.getId(), configWord.getCode())) {
            return ApiResponse.fail("修改保存配置字'" + configWord.getCode() + "'失败，配置字代码已存在");
        }
        ConfigWordPo configWordPo = ConfigWordMptAssembler.INSTANCE.toPo(configWord);
        configWordPo.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(configWordAppService.modifyConfigWord(configWordPo));
    }

    /**
     * 修改保存配置字配置文件
     *
     * @param configWordCode    配置字代码
     * @param configWordProfile 配置字配置文件
     * @return 结果
     */
    @Log(title = "配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:dota:configWord:edit")
    @PutMapping("/{configWordCode}/profile")
    public ApiResponse<Integer> editProfile(@PathVariable String configWordCode, @Validated @RequestBody ConfigWordProfileMpt configWordProfile) {
        log.info("管理后台用户[{}]修改保存配置字[{}]配置文件[{}]", SecurityUtils.getUsername(), configWordCode, configWordProfile.getCode());
        if (!configWordAppService.checkProfileCodeUnique(configWordProfile.getId(), configWordCode, configWordProfile.getCode())) {
            return ApiResponse.fail("修改保存配置字配置文件'" + configWordProfile.getCode() + "'失败，配置字配置文件代码已存在");
        }
        ConfigWordProfilePo configWordProfilePo = ConfigWordProfileMptAssembler.INSTANCE.toPo(configWordProfile);
        configWordProfilePo.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(configWordAppService.modifyConfigWordProfile(configWordCode, configWordProfilePo));
    }

    /**
     * 修改保存配置字字段
     *
     * @param configWordCode  配置字代码
     * @param configWordField 配置字字段
     * @return 结果
     */
    @Log(title = "配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:dota:configWord:edit")
    @PutMapping("/{configWordCode}/profile/{configWordProfileCode}/field")
    public ApiResponse<Integer> editField(@PathVariable String configWordCode, @PathVariable String configWordProfileCode,
                                          @Validated @RequestBody ConfigWordFieldMpt configWordField) {
        log.info("管理后台用户[{}]修改保存配置字[{}]配置文件[{}]字段[{}]", SecurityUtils.getUsername(), configWordCode,
                configWordProfileCode, configWordField.getCode());
        if (!configWordAppService.checkFieldCodeUnique(configWordField.getId(), configWordCode, configWordProfileCode,
                configWordField.getCode())) {
            return ApiResponse.fail("修改保存配置字字段'" + configWordField.getCode() + "'失败，配置字字段代码已存在");
        }
        ConfigWordFieldPo configWordFieldPo = ConfigWordFieldMptAssembler.INSTANCE.toPo(configWordField);
        configWordFieldPo.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(configWordAppService.modifyConfigWordField(configWordCode, configWordFieldPo));
    }

    /**
     * 删除配置字
     *
     * @param configWordIds 配置字ID数组
     * @return 结果
     */
    @Log(title = "配置字管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:dota:configWord:remove")
    @DeleteMapping("/{configWordIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] configWordIds) {
        log.info("管理后台用户[{}]删除配置字[{}]", SecurityUtils.getUsername(), configWordIds);
        return ApiResponse.ok(configWordAppService.deleteConfigWordByIds(configWordIds));
    }

    /**
     * 删除配置字配置文件
     *
     * @param configWordCode       配置字代码
     * @param configWordProfileIds 配置字配置文件ID数组
     * @return 结果
     */
    @Log(title = "配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:dota:configWord:edit")
    @DeleteMapping("/{configWordCode}/field/{configWordProfileIds}")
    public ApiResponse<Integer> removeProfile(@PathVariable String configWordCode, @PathVariable Long[] configWordProfileIds) {
        log.info("管理后台用户[{}]删除配置字[{}]配置文件[{}]", SecurityUtils.getUsername(), configWordCode, configWordProfileIds);
        return ApiResponse.ok(configWordAppService.deleteConfigWordProfileByIds(configWordCode, configWordProfileIds));
    }

    /**
     * 删除配置字字段
     *
     * @param configWordCode     配置字代码
     * @param configWordFieldIds 配置字字段ID数组
     * @return 结果
     */
    @Log(title = "配置字管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:dota:configWord:edit")
    @DeleteMapping("/{configWordCode}/profile/{configWordProfileCode}/field/{configWordFieldIds}")
    public ApiResponse<Integer> removeField(@PathVariable String configWordCode, @PathVariable String configWordProfileCode,
                                             @PathVariable Long[] configWordFieldIds) {
        log.info("管理后台用户[{}]删除配置字[{}]配置文件[{}]字段[{}]", SecurityUtils.getUsername(), configWordCode,
                configWordProfileCode, configWordFieldIds);
        return ApiResponse.ok(configWordAppService.deleteConfigWordFieldByIds(configWordCode, configWordProfileCode, configWordFieldIds));
    }
}
