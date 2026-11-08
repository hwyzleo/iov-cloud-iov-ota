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
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleActivityRelationMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleRelationTaskQueryMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleTaskRelationMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.VehicleProjectionMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.VehicleRelationMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.query.VehicleRelationTaskQuery;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.InventoryProcessSummary;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleActivityRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleManagementResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleDeviceInventoryQueryService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleInventoryQueryService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleUpgradeRelationQueryService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

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
    private final VehicleInventoryQueryService vehicleInventoryQueryService;
    private final VehicleDeviceInventoryQueryService vehicleDeviceInventoryQueryService;
    private final VehicleUpgradeRelationQueryService vehicleUpgradeRelationQueryService;

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
        // CR-022：LEFT JOIN 运行态一次取回 lastInventoryReportedAt，不得逐 VIN 查询
        List<VehicleManagementResult> vehicleList = vehicleAppService.search(vehicle.getVin(), getBeginTime(vehicle), getEndTime(vehicle));
        return ApiResponse.ok(getPageResult(PageUtil.convert(vehicleList, VehicleProjectionMptAssembler.INSTANCE::fromResult)));
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
        return ApiResponse.ok(VehicleProjectionMptAssembler.INSTANCE.fromResult(vehicleAppService.getVehicleByVin(vin)));
    }

    /**
     * 车辆软件清单详情（CR-019 §10：ECU → SoftwareUnits 层级）
     *
     * @param vin 车架号
     * @return 已接受清单详情（含 model / canonical digest / software units）
     */
    @RequiresPermissions("ota:fota:vehicle:query")
    @GetMapping(value = "/{vin}/inventory")
    public ApiResponse<InventoryProcessSummary> inventory(@PathVariable String vin) {
        log.info("管理后台用户[{}]查询车辆[{}]软件清单详情", SecurityUtils.getUsername(), vin);
        vehicleAppService.requireVehicleExists(vin);
        return ApiResponse.ok(vehicleDeviceInventoryQueryService.getLatestByVin(vin));
    }

    /**
     * 车辆关联升级活动（CR-023 §4.1：以已生成 VehicleTask 为依据，同一活动仅一条）
     *
     * @param vin 车架号
     * @return 活动关联分页结果
     */
    @RequiresPermissions("ota:fota:activity:list")
    @GetMapping(value = "/{vin}/activities")
    public ApiResponse<PageResult<VehicleActivityRelationMpt>> activities(@PathVariable String vin) {
        log.info("管理后台用户[{}]查询车辆[{}]关联升级活动", SecurityUtils.getUsername(), vin);
        vehicleAppService.requireVehicleExists(vin);
        startPage();
        // CR-023 §4.1：一次分组关联查询，禁止逐行 getTaskById/getActivityById
        List<VehicleActivityRelationResult> list = vehicleUpgradeRelationQueryService.listActivities(vin);
        return ApiResponse.ok(getPageResult(PageUtil.convert(list, VehicleRelationMptAssembler::toActivityVo)));
    }

    /**
     * 车辆关联升级任务（CR-023 §4.2：全部历史 VehicleTask，默认创建时间倒序）
     *
     * @param vin   车架号
     * @param query 查询条件（活动/任务状态/VehicleTask 状态/时间范围）
     * @return 任务关联分页结果
     */
    @RequiresPermissions("ota:fota:taskVehicle:list")
    @GetMapping(value = "/{vin}/tasks")
    public ApiResponse<PageResult<VehicleTaskRelationMpt>> tasks(@PathVariable String vin,
                                                                 VehicleRelationTaskQueryMpt query) {
        log.info("管理后台用户[{}]查询车辆[{}]关联升级任务", SecurityUtils.getUsername(), vin);
        vehicleAppService.requireVehicleExists(vin);
        startPage();
        VehicleRelationTaskQuery condition = VehicleRelationTaskQuery.builder()
                .vin(vin)
                .activityId(query.getActivityId())
                .taskState(query.getTaskState())
                .vehicleTaskStatus(query.getVehicleTaskStatus())
                .beginTime(getBeginTime(query))
                .endTime(getEndTime(query))
                .build();
        List<VehicleTaskRelationResult> list = vehicleUpgradeRelationQueryService.listTasks(condition);
        return ApiResponse.ok(getPageResult(PageUtil.convert(list, VehicleRelationMptAssembler::toTaskVo)));
    }

    /**
     * 车辆软件清单导出（CR-019 §10：CSV 每行一个 VIN + ECU + Target + Slot）
     *
     * @param response 响应
     * @param vin      车架号
     */
    @Log(title = "车辆软件清单导出", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:vehicle:export")
    @PostMapping(value = "/{vin}/inventory/export")
    public void exportInventory(HttpServletResponse response, @PathVariable String vin) throws IOException {
        log.info("管理后台用户[{}]导出车辆[{}]软件清单", SecurityUtils.getUsername(), vin);
        vehicleAppService.requireVehicleExists(vin);
        List<VehicleInventoryQueryService.ExportRow> rows = vehicleInventoryQueryService.exportByVin(vin);
        String header = "vin,ecu_id,hardware_pn,hardware_version,software_model,software_target_code,software_part_number,sw_version,slot,active\n";
        String body = rows.stream()
                .map(r -> String.join(",",
                        csv(r.getVin()), csv(r.getEcuId()), csv(r.getHardwarePn()),
                        csv(r.getHardwareVersion()), csv(r.getSoftwareModel()),
                        csv(r.getSoftwareTargetCode()), csv(r.getSoftwarePartNumber()),
                        csv(r.getSwVersion()), csv(r.getSlot()),
                        r.getActive() == null ? "" : r.getActive().toString()))
                .collect(Collectors.joining("\n"));
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=vehicle_inventory_" + vin + ".csv");
        response.getOutputStream().write((header + body).getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
