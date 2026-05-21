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
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ActivityPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskVehiclePo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆升级任务相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/mpt/taskVehicle")
public class TaskVehicleMptController extends BaseController {

    private final TaskAppService taskAppService;
    private final ActivityAppService activityAppService;
    private final TaskVehicleAppService taskVehicleAppService;

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
            TaskPo task = taskAppService.getTaskById(taskVehicleMpt.getTaskId());
            if (task != null) {
                taskVehicleMpt.setTaskName(task.getName());
            }
            ActivityPo activity = activityAppService.getActivityById(taskVehicleMpt.getActivityId());
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
     * 获取车辆升级任务过程
     *
     * @param taskVehicleId 升级任务车辆ID
     * @return 升级任务过程列表
     */
    @RequiresPermissions("ota:fota:taskVehicle:query")
    @GetMapping(value = "/{taskVehicleId}/process")
    public ApiResponse<TaskVehicleMpt> listProcess(@PathVariable Long taskVehicleId) {
        log.info("管理后台用户[{}]获取车辆升级任务[{}]过程", SecurityUtils.getUsername(), taskVehicleId);
        return null;
    }
}
