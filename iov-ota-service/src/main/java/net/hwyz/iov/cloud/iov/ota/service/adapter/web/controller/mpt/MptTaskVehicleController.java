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
import net.hwyz.iov.cloud.iov.ota.service.application.dto.query.VehicleRelationTaskQuery;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskRelationResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskVehicleAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleUpgradeRelationQueryService;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskVehicleProcessResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionProcessView;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionEventProcessView;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskVehicleRetryLogResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.UpgradeLogResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskVehicleProcessQueryService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleTaskConsentQueryService;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskConsentCurrentResult;
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

    private final TaskVehicleAppService taskVehicleAppService;
    private final TaskVehicleProcessQueryService taskVehicleProcessQueryService;
    private final VehicleTaskConsentQueryService vehicleTaskConsentQueryService;
    private final VehicleUpgradeRelationQueryService vehicleUpgradeRelationQueryService;

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
        // CR-023 §4.2：一次 JOIN 取回任务/活动名称，消除逐行 getTaskById/getActivityById 与空任务 NPE
        // vin / vehicleTaskStatus 均为可选过滤，不带则返回全部车辆任务
        VehicleRelationTaskQuery condition = VehicleRelationTaskQuery.builder()
                .vin(taskVehicle.getVin())
                .vehicleTaskStatus(taskVehicle.getVehicleTaskStatus())
                .beginTime(getBeginTime(taskVehicle))
                .endTime(getEndTime(taskVehicle))
                .build();
        List<VehicleTaskRelationResult> relations = vehicleUpgradeRelationQueryService.listTasks(condition);
        List<TaskVehicleMpt> taskVehicleMptList = PageUtil.convert(relations, relation -> TaskVehicleMpt.builder()
                .id(relation.getVehicleTaskId())
                .activityId(relation.getActivityId())
                .activityName(relation.getActivityName())
                .taskId(relation.getTaskId())
                .taskName(relation.getTaskName())
                .vin(relation.getVin())
                .vehicleTaskStatus(relation.getVehicleTaskStatus())
                .resultCode(relation.getResultCode())
                .createTime(relation.getCreateTime())
                .build());
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

    /**
     * 单 VehicleTask 的不可变授权历史（CR-016 §6、US-102～105）
     */
    @RequiresPermissions("ota:fota:taskVehicle:query")
    @GetMapping(value = "/{taskVehicleId}/consents")
    public ApiResponse<PageResult<VehicleTaskConsentResult>> consents(@PathVariable Long taskVehicleId) {
        log.info("管理后台用户[{}]查询车辆升级任务[{}]授权历史", SecurityUtils.getUsername(), taskVehicleId);
        startPage();
        List<VehicleTaskConsentResult> list = vehicleTaskConsentQueryService.listHistory(taskVehicleId);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 单 VehicleTask 当前权威授权状态（CR-016 §6、US-102～105）
     */
    @RequiresPermissions("ota:fota:taskVehicle:query")
    @GetMapping(value = "/{taskVehicleId}/consents/current")
    public ApiResponse<VehicleTaskConsentCurrentResult> currentConsent(@PathVariable Long taskVehicleId) {
        log.info("管理后台用户[{}]查询车辆升级任务[{}]当前授权状态", SecurityUtils.getUsername(), taskVehicleId);
        return ApiResponse.ok(vehicleTaskConsentQueryService.getCurrent(taskVehicleId));
    }
}
