package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.ccp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.iov.ota.api.contract.TaskVehicleProcessCcp;
import net.hwyz.iov.cloud.iov.ota.api.contract.TaskVehicleStateCcp;
import net.hwyz.iov.cloud.iov.ota.api.vo.CloudFotaInfoCcp;
import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleFotaInfoCcp;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.DeviceInfoCcpAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskVehicleAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.DeviceInfoVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ActivityRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TaskService;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.TaskVehicleProcessCcpAssembler;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.util.FotaHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/ccp/fota")
public class CcpFotaController extends BaseController {

    private final TaskService taskService;
    private final VehicleRepository vehicleRepository;
    private final FotaHelper fotaHelper;
    private final ActivityRepository activityRepository;
    private final TaskVehicleRepository taskVehicleRepository;
    private final TaskVehicleAppService taskVehicleAppService;

    @PostMapping("/check")
    public ApiResponse<CloudFotaInfoCcp> check(@RequestHeader String vin, @Validated @RequestBody VehicleFotaInfoCcp vehicleFotaInfo) {
        log.info("车辆[{}]检查车辆升级信息", vin);
        VehicleDo vehicle = vehicleRepository.getById(vin).orElseThrow(() -> new VehicleNotExistException(vin));
        List<DeviceInfoVo> deviceInfoList = DeviceInfoCcpAssembler.INSTANCE.toVoList(vehicleFotaInfo.getDeviceInfoList());
        if (vehicle.checkBaseline(vehicleFotaInfo.getBaseline()) || vehicle.checkDevices(deviceInfoList)) {
            vehicle.markBaselineAlignment(fotaHelper.isBaselineAlignment(vehicleFotaInfo.getBaseline(), deviceInfoList));
        }
        final CloudFotaInfoCcp[] cloudFotaInfoCcp = {null};
        taskService.getVehicleTask(vehicle).ifPresent(task -> {
            if (!task.checkPreconditions(vehicle)) {
                return;
            }
            activityRepository.getById(task.getActivityId().getValue()).ifPresent(activity -> {
                if (!activity.checkPreconditions(vehicle)) {
                    return;
                }
                taskVehicleRepository.getByTaskIdAndVin(task.getId().getValue(), vin).ifPresent(taskVehicle -> {
                    taskVehicle.loadBaseInfo(activity, task);
                    taskVehicle.loadStrategy(task);
                    taskVehicle.loadSoftwareBuildVersion(activity, task, vehicle, fotaHelper);
                    taskVehicle.loadArticle(activity);
                    cloudFotaInfoCcp[0] = taskVehicle.toCloudFotaInfoCcp();
                    taskVehicleRepository.save(taskVehicle);
                });
            });
        });
        vehicleRepository.save(vehicle);
        return ApiResponse.ok(cloudFotaInfoCcp[0]);
    }

    @PostMapping("/reportTaskProcess")
    public ApiResponse<Void> reportTaskProcess(@RequestHeader String vin, @Validated @RequestBody TaskVehicleProcessCcp taskVehicleProcess) {
        log.info("车辆[{}]上报车辆升级任务过程", vin);
        VehicleDo vehicle = vehicleRepository.getById(vin).orElseThrow(() -> new VehicleNotExistException(vin));
        taskService.getVehicleTask(vehicle).ifPresent(task -> {
            if (task.getId().getValue() != taskVehicleProcess.getTaskId()) {
                log.warn("车辆[{}]上报车辆升级任务状态任务ID不一致", vin);
                return;
            }
            taskVehicleRepository.getByTaskIdAndVin(task.getId().getValue(), vin).ifPresent(taskVehicle -> {
                taskVehicleAppService.addTaskVehicleProcess(TaskVehicleProcessCcpAssembler.INSTANCE.toPo(taskVehicleProcess));
            });
        });
        return ApiResponse.ok();
    }

    @PostMapping("/reportTaskState")
    public ApiResponse<Void> reportTaskState(@RequestHeader String vin, @Validated @RequestBody TaskVehicleStateCcp taskVehicleState) {
        log.info("车辆[{}]上报车辆升级任务状态", vin);
        VehicleDo vehicle = vehicleRepository.getById(vin).orElseThrow(() -> new VehicleNotExistException(vin));
        taskService.getVehicleTask(vehicle).ifPresent(task -> {
            if (task.getId().getValue() != taskVehicleState.getTaskId()) {
                log.warn("车辆[{}]上报车辆升级任务状态任务ID不一致", vin);
                return;
            }
            taskVehicleRepository.getByTaskIdAndVin(task.getId().getValue(), vin).ifPresent(taskVehicle -> {
                taskVehicle.updateState(taskVehicleState.getTaskState());
                taskVehicleRepository.save(taskVehicle);
            });
        });
        return ApiResponse.ok();
    }
}