package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.InstallConditionTypeResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.InstallConditionType;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.InstallConditionTypeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台安装条件类型控制器
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/install-condition-type/v1")
public class MptInstallConditionTypeController {

    private final InstallConditionTypeRepository installConditionTypeRepository;

    /**
     * 获取所有安装条件类型
     */
    @RequiresPermissions("ota:fota:task:list")
    @GetMapping(value = "/list")
    public ApiResponse<List<InstallConditionTypeResult>> list() {
        log.info("管理后台用户[{}]查询所有安装条件类型", SecurityUtils.getUsername());
        List<InstallConditionType> types = installConditionTypeRepository.listAll();
        return ApiResponse.ok(toResultList(types));
    }

    /**
     * 获取指定阶段的安装条件类型
     */
    @RequiresPermissions("ota:fota:task:list")
    @GetMapping(value = "/listByPhase/{phase}")
    public ApiResponse<List<InstallConditionTypeResult>> listByPhase(@PathVariable String phase) {
        log.info("管理后台用户[{}]查询阶段[{}]的安装条件类型", SecurityUtils.getUsername(), phase);
        List<InstallConditionType> types = installConditionTypeRepository.listByPhase(phase);
        return ApiResponse.ok(toResultList(types));
    }

    /**
     * 获取必选的安装条件类型
     */
    @RequiresPermissions("ota:fota:task:list")
    @GetMapping(value = "/listMandatory")
    public ApiResponse<List<InstallConditionTypeResult>> listMandatory() {
        log.info("管理后台用户[{}]查询必选安装条件类型", SecurityUtils.getUsername());
        List<InstallConditionType> types = installConditionTypeRepository.listMandatory();
        return ApiResponse.ok(toResultList(types));
    }

    private List<InstallConditionTypeResult> toResultList(List<InstallConditionType> types) {
        return types.stream()
            .map(t -> InstallConditionTypeResult.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .unit(t.getUnit())
                .valueType(t.getValueType())
                .defaultValue(t.getDefaultValue())
                .applicablePhase(t.getApplicablePhase())
                .mandatory(t.getMandatory())
                .description(t.getDescription())
                .build())
            .collect(Collectors.toList());
    }
}
