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
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskVehicleMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.TaskVehicleMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ActivityAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskVehicleAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskVehicleProcessResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionProcessView;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionEventProcessView;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskVehicleRetryLogResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.UpgradeLogResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskVehicleProcessQueryService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 车辆升级任务相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/taskVehicle/v1")
public class MptTaskVehicleController extends BaseController {

    private final TaskAppService taskAppService;
    private final ActivityAppService activityAppService;
    private final TaskVehicleAppService taskVehicleAppService;
    private final TaskVehicleProcessQueryService taskVehicleProcessQueryService;

    /**
     * 分页查询车辆升级任务
     *
     * @param taskVehicle 车辆升级任务
     * @return 车辆升级任务列表
     */
    @RequiresPermissions("ota:fota:taskVehicle:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<TaskVehicleMpt>> list(TaskVehicleMpt taskVehicle) {
        log.info("管理后台用户[{}]分页车辆查询升级任务", SecurityUtils.getUsername());
        startPage();
        List<TaskVehiclePo> taskVehiclePoList = taskVehicleAppService.search(taskVehicle.getVin(), getBeginTime(taskVehicle), getEndTime(taskVehicle));
        List<TaskVehicleMpt> taskVehicleMptList = PageUtil.convert(taskVehiclePoList, TaskVehicleMptAssembler.INSTANCE::fromPo);
taskVehicleMptList.forEach(taskVehicleMpt -> {
            TaskResult task = taskAppService.getTaskById(taskVehicleMpt.getTaskId());
            if (task != null) {
                taskVehicleMpt.setTaskName(task.getName());
            }
            ActivityPo activity = activityAppService.getActivityById(task.getActivityId());
            if (activity != null) {
                taskVehicleMpt.setActivityName(activity.getName());
            }
        });
        return ApiResponse.ok(getPageResult(taskVehicleMptList));
    }

    /**
     * 导出车辆升级任务
     *
     * @param response    响应
     * @param taskVehicle 车辆升级任务
     */
    @Log(title = "车辆升级任务管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:taskVehicle:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaskVehicleMpt taskVehicle) {
        log.info("管理后台用户[{}]导出车辆升级任务", SecurityUtils.getUsername());
    }

    /**
     * 根据车辆升级任务ID获取车辆升级任务
     *
     * @param taskVehicleId 车辆升级任务ID
     * @return 车辆升级任务
     */
    @RequiresPermissions("ota:fota:taskVehicle:query")
    @GetMapping(value = "/{taskVehicleId}")
    public ApiResponse<TaskVehicleMpt> getInfo(@PathVariable Long taskVehicleId) {
        log.info("管理后台用户[{}]根据车辆升级任务ID[{}]获取车辆升级任务", SecurityUtils.getUsername(), taskVehicleId);
        TaskVehiclePo taskVehiclePo = taskVehicleAppService.getTaskVehicleById(taskVehicleId);
        return ApiResponse.ok(TaskVehicleMptAssembler.INSTANCE.fromPo(taskVehiclePo));
    }

    /**
     * 获取车辆升级任务完整过程（CR-015 §3.3）
     *
     * @param taskVehicleId 升级任务车辆ID
     * @return 完整过程视图（含清单/授权/包/执行/控制/ECU结果/日志/技术投递）
     */
    @RequiresPermissions("ota:fota:taskVehicle:query")
    @GetMapping(value = "/{taskVehicleId}/process")
    public ApiResponse<TaskVehicleProcessResult> listProcess(@PathVariable Long taskVehicleId) {
        log.info("管理后台用户[{}]获取车辆升级任务[{}]完整过程", SecurityUtils.getUsername(), taskVehicleId);
        return ApiResponse.ok(taskVehicleProcessQueryService.getProcess(taskVehicleId));
    }

    /**
     * 车辆任务的安装尝试列表（CR-015 §3.3 分页子资源）
     */
    @RequiresPermissions("ota:fota:taskVehicle:query")
    @GetMapping(value = "/{taskVehicleId}/executions")
    public ApiResponse<PageResult<ExecutionProcessView>> executions(@PathVariable Long taskVehicleId) {
        log.info("管理后台用户[{}]查询车辆升级任务[{}]安装尝试", SecurityUtils.getUsername(), taskVehicleId);
        startPage();
        List<ExecutionProcessView> list = taskVehicleProcessQueryService.listExecutions(taskVehicleId);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 单次执行的事件列表（CR-015 §3.3 分页子资源）
     */
    @RequiresPermissions("ota:fota:taskVehicle:query")
    @GetMapping(value = "/{taskVehicleId}/executions/{executionId}/events")
    public ApiResponse<PageResult<ExecutionEventProcessView>> executionEvents(
            @PathVariable Long taskVehicleId, @PathVariable Long executionId) {
        log.info("管理后台用户[{}]查询执行[{}]事件列表", SecurityUtils.getUsername(), executionId);
        startPage();
        List<ExecutionEventProcessView> list = taskVehicleProcessQueryService.listExecutionEvents(executionId);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 车辆任务重试/续传轨迹（CR-015 §3.3 分页子资源 / §3.4 审计筛选）
     */
    @RequiresPermissions("ota:fota:taskVehicle:query")
    @GetMapping(value = "/{taskVehicleId}/retryLogs")
    public ApiResponse<PageResult<TaskVehicleRetryLogResult>> retryLogs(
            @PathVariable Long taskVehicleId,
            @RequestParam(required = false) Date beginTime,
            @RequestParam(required = false) Date endTime,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Integer attemptNo) {
        log.info("管理后台用户[{}]查询车辆升级任务[{}]重试/续传轨迹", SecurityUtils.getUsername(), taskVehicleId);
        startPage();
        List<TaskVehicleRetryLogResult> list = taskVehicleProcessQueryService
                .listRetryLogs(taskVehicleId, beginTime, endTime, stage, result, attemptNo);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 车辆升级日志登记（CR-015 §3.3 分页子资源 / §3.4 审计筛选）
     */
    @RequiresPermissions("ota:fota:taskVehicle:query")
    @GetMapping(value = "/{taskVehicleId}/upgradeLogs")
    public ApiResponse<PageResult<UpgradeLogResult>> upgradeLogs(
            @PathVariable Long taskVehicleId,
            @RequestParam(required = false) Date beginTime,
            @RequestParam(required = false) Date endTime,
            @RequestParam(required = false) String uploadState) {
        log.info("管理后台用户[{}]查询车辆升级任务[{}]升级日志", SecurityUtils.getUsername(), taskVehicleId);
        startPage();
        List<UpgradeLogResult> list = taskVehicleProcessQueryService
                .listUpgradeLogs(taskVehicleId, beginTime, endTime, uploadState);
        return ApiResponse.ok(getPageResult(list));
    }
}
