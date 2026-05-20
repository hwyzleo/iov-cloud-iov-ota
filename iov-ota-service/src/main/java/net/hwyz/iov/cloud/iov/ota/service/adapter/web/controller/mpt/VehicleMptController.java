package net.hwyz.iov.cloud.iov.ota.service.facade.mpt;

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
import net.hwyz.iov.cloud.iov.ota.api.contract.VehicleMpt;
import net.hwyz.iov.cloud.iov.ota.api.feign.mpt.VehicleMptApi;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.VehicleMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.VehStatusPo;
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
@RequestMapping(value = "/mpt/vehicle")
public class VehicleMptController extends BaseController {

    private final VehicleAppService vehicleAppService;

    /**
     * 分页查询车辆信息
     *
     * @param vehicle 车辆信息
     * @return 车辆信息列表
     */
    @RequiresPermissions("ota:fota:vehicle:list")
    @Override
    @GetMapping(value = "/list")
    public TableDataInfo list(VehicleMpt vehicle) {
        logger.info("管理后台用户[{}]分页查询车辆信息", SecurityUtils.getUsername());
        startPage();
        List<VehStatusPo> vehiclePoList = vehicleAppService.search(vehicle.getVin(), getBeginTime(vehicle), getEndTime(vehicle));
        List<VehicleMpt> vehicleMptList = VehicleMptAssembler.INSTANCE.fromPoList(vehiclePoList);
        return getDataTable(vehiclePoList, vehicleMptList);
    }

    /**
     * 导出车辆信息
     *
     * @param response 响应
     * @param vehicle  车辆信息
     */
    @Log(title = "升级车辆管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:vehicle:export")
    @Override
    @PostMapping("/export")
    public void export(HttpServletResponse response, VehicleMpt vehicle) {
        logger.info("管理后台用户[{}]导出车辆信息", SecurityUtils.getUsername());
    }

    /**
     * 根据车架号获取车辆信息
     *
     * @param vin 车架号
     * @return 车辆信息
     */
    @RequiresPermissions("ota:fota:vehicle:query")
    @Override
    @GetMapping(value = "/{vin}")
    public AjaxResult getInfo(@PathVariable String vin) {
        logger.info("管理后台用户[{}]根据车架号[{}]获取车辆信息", SecurityUtils.getUsername(), vin);
        return success(VehicleMptAssembler.INSTANCE.fromPo(vehicleAppService.getVehicleByVin(vin)));
    }

}
