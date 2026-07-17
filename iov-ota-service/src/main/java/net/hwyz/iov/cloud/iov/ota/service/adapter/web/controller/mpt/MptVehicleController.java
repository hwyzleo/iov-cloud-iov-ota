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
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.VehicleProjectionMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/vehicle/v1")
public class MptVehicleController extends BaseController {

    private final VehicleAppService vehicleAppService;

    /**
     * 分页查询车辆信息
     *
     * @param vehicle 车辆信息
     * @return 车辆信息列表
     */
    @RequiresPermissions("ota:fota:vehicle:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<VehicleMpt>> list(VehicleMpt vehicle) {
        log.info("管理后台用户[{}]分页查询车辆信息", SecurityUtils.getUsername());
        startPage();
        List<VehicleProjectionPo> vehiclePoList = vehicleAppService.search(vehicle.getVin(), getBeginTime(vehicle), getEndTime(vehicle));
        return ApiResponse.ok(getPageResult(PageUtil.convert(vehiclePoList, VehicleProjectionMptAssembler.INSTANCE::fromPo)));
    }

    /**
     * 导出车辆信息
     *
     * @param response 响应
     * @param vehicle  车辆信息
     */
    @Log(title = "升级车辆管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:vehicle:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VehicleMpt vehicle) {
        log.info("管理后台用户[{}]导出车辆信息", SecurityUtils.getUsername());
    }

    /**
     * 根据车架号获取车辆信息
     *
     * @param vin 车架号
     * @return 车辆信息
     */
    @RequiresPermissions("ota:fota:vehicle:query")
    @GetMapping(value = "/{vin}")
    public ApiResponse<VehicleMpt> getInfo(@PathVariable String vin) {
        log.info("管理后台用户[{}]根据车架号[{}]获取车辆信息", SecurityUtils.getUsername(), vin);
        return ApiResponse.ok(VehicleProjectionMptAssembler.INSTANCE.fromPo(vehicleAppService.getVehicleByVin(vin)));
    }

}
