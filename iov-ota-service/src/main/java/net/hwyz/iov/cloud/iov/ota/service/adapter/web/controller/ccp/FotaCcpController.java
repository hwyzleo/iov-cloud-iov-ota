package net.hwyz.iov.cloud.iov.ota.service.facade.ccp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.Response;
import net.hwyz.iov.cloud.framework.common.web.controller.BaseController;
import net.hwyz.iov.cloud.iov.ota.api.contract.CloudFotaInfoCcp;
import net.hwyz.iov.cloud.iov.ota.api.contract.TaskVehicleProcessCcp;
import net.hwyz.iov.cloud.iov.ota.api.contract.TaskVehicleStateCcp;
import net.hwyz.iov.cloud.iov.ota.api.contract.VehicleFotaInfoCcp;
import net.hwyz.iov.cloud.iov.ota.api.feign.ccp.FotaCcpService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskVehicleAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.activity.repository.ActivityRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.service.TaskService;
import net.hwyz.iov.cloud.iov.ota.service.domain.taskvehicle.repository.TaskVehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model.DeviceInfoVo;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.model.VehicleDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.vehicle.repository.VehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.DeviceInfoCcpAssembler;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.TaskVehicleProcessCcpAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.util.FotaHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 在线固件升级相关中奖计算平台接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/ccp/fota")
public class FotaCcpController extends BaseController implements FotaCcpService {

    private final TaskService taskService;
    private final VehicleRepository vehicleRepository;
    private final FotaHelper fotaHelper;
    private final ActivityRepository activityRepository;
    private final TaskVehicleRepository taskVehicleRepository;
    private final TaskVehicleAppService taskVehicleAppService;

    /**
     * 检查车辆升级信息
     *
     * @param vin             车架号
     * @param vehicleFotaInfo 车辆升级信息
     * @return 云端升级信息
     */
    @Override
    @PostMapping("/check")
    public Response<CloudFotaInfoCcp> check(@RequestHeader String vin, @Validated @RequestBody VehicleFotaInfoCcp vehicleFotaInfo) {
        logger.info("车辆[{}]检查车辆升级信息", vin);
        // 处理车辆信息
        VehicleDo vehicle = vehicleRepository.getById(vin).orElseThrow(() -> new VehicleNotExistException(vin));
        List<DeviceInfoVo> deviceInfoList = DeviceInfoCcpAssembler.INSTANCE.toVoList(vehicleFotaInfo.getDeviceInfoList());
        if (vehicle.checkBaseline(vehicleFotaInfo.getBaseline()) || vehicle.checkDevices(deviceInfoList)) {
            vehicle.markBaselineAlignment(fotaHelper.isBaselineAlignment(vehicleFotaInfo.getBaseline(), deviceInfoList));
        }
        // 处理任务信息
        final CloudFotaInfoCcp[] cloudFotaInfoCcp = {null};
        taskService.getVehicleTask(vehicle).ifPresent(task -> {
            if (!task.checkPreconditions(vehicle)) {
                return;
            }
            activityRepository.getById(task.getActivityId()).ifPresent(activity -> {
                if (!activity.checkPreconditions(vehicle)) {
                    return;
                }
                taskVehicleRepository.getByTaskIdAndVin(task.getId(), vin).ifPresent(taskVehicle -> {
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
        return new Response<>(cloudFotaInfoCcp[0]);
    }

    /**
     * 上报车辆升级任务过程
     *
     * @param vin                车架号
     * @param taskVehicleProcess 车辆升级任务过程
     * @return 上报结果
     */
    @Override
    @PostMapping("/reportTaskProcess")
    public Response<Void> reportTaskProcess(@RequestHeader String vin, @Validated @RequestBody TaskVehicleProcessCcp taskVehicleProcess) {
        logger.info("车辆[{}]上报车辆升级任务过程", vin);
        VehicleDo vehicle = vehicleRepository.getById(vin).orElseThrow(() -> new VehicleNotExistException(vin));
        taskService.getVehicleTask(vehicle).ifPresent(task -> {
            if (task.getId().longValue() != taskVehicleProcess.getTaskId()) {
                logger.warn("车辆[{}]上报车辆升级任务状态任务ID不一致", vin);
                return;
            }
            taskVehicleRepository.getByTaskIdAndVin(task.getId(), vin).ifPresent(taskVehicle -> {
                taskVehicleAppService.addTaskVehicleProcess(TaskVehicleProcessCcpAssembler.INSTANCE.toPo(taskVehicleProcess));
                // TODO 整体结束时更新车辆零件版本
            });
        });
        return new Response<>();
    }

    /**
     * 上报车辆升级任务状态
     *
     * @param vin              车架号
     * @param taskVehicleState 车辆升级任务状态
     * @return 上报结果
     */
    @Override
    @PostMapping("/reportTaskState")
    public Response<Void> reportTaskState(String vin, TaskVehicleStateCcp taskVehicleState) {
        logger.info("车辆[{}]上报车辆升级任务状态", vin);
        VehicleDo vehicle = vehicleRepository.getById(vin).orElseThrow(() -> new VehicleNotExistException(vin));
        taskService.getVehicleTask(vehicle).ifPresent(task -> {
            if (task.getId().longValue() != taskVehicleState.getTaskId()) {
                logger.warn("车辆[{}]上报车辆升级任务状态任务ID不一致", vin);
                return;
            }
            taskVehicleRepository.getByTaskIdAndVin(task.getId(), vin).ifPresent(taskVehicle -> {
                taskVehicle.updateState(taskVehicleState.getTaskState());
                taskVehicleRepository.save(taskVehicle);
            });
        });
        return new Response<>();
    }
}
