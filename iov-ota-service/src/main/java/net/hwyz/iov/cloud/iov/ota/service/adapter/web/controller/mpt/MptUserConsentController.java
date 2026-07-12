package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.iov.ota.api.vo.UserConsentMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.UserConsentMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.UserConsentAppService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户授权相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/userConsent/v1")
public class MptUserConsentController extends BaseController {

    private final UserConsentAppService userConsentAppService;

    /**
     * 查询用户授权记录
     *
     * @param taskId 升级任务ID
     * @param vin    车架号
     * @return 授权记录列表
     */
    @RequiresPermissions("ota:fota:taskVehicle:list")
    @GetMapping(value = "/list")
    public ApiResponse<List<UserConsentMpt>> list(@RequestParam(required = false) Long taskId,
                                                    @RequestParam(required = false) String vin) {
        log.info("管理后台用户[{}]查询用户授权记录 taskId[{}] vin[{}]", SecurityUtils.getUsername(), taskId, vin);
        return ApiResponse.ok(UserConsentMptAssembler.INSTANCE.fromPoList(userConsentAppService.search(taskId, vin)));
    }

    /**
     * 根据ID获取用户授权记录
     *
     * @param id 主键ID
     * @return 授权记录
     */
    @RequiresPermissions("ota:fota:taskVehicle:list")
    @GetMapping(value = "/{id}")
    public ApiResponse<UserConsentMpt> getInfo(@PathVariable Long id) {
        log.info("管理后台用户[{}]查询用户授权记录[{}]", SecurityUtils.getUsername(), id);
        return ApiResponse.ok(UserConsentMptAssembler.INSTANCE.fromPo(userConsentAppService.getUserConsentById(id)));
    }

}
