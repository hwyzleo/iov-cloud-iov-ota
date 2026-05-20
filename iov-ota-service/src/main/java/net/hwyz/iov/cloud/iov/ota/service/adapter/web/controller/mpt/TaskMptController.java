package net.hwyz.iov.cloud.iov.ota.service.facade.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.framework.common.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.common.web.domain.AjaxResult;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.iov.ota.api.contract.TaskAuditMpt;
import net.hwyz.iov.cloud.iov.ota.api.contract.TaskMpt;
import net.hwyz.iov.cloud.iov.ota.api.contract.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.feign.mpt.TaskMptApi;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.model.TaskDo;
import net.hwyz.iov.cloud.iov.ota.service.domain.task.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.TaskMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.exception.TaskNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskRestrictionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.TaskStrategyPo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskRestrictionType.*;
import static net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskStrategyType.*;

/**
 * 升级任务相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/mpt/task")
public class TaskMptController extends BaseController {

    private final CacheService cacheService;
    private final TaskAppService taskAppService;
    private final TaskRepository taskRepository;

    /**
     * 分页查询升级任务
     *
     * @param task 升级任务
     * @return 升级任务列表
     */
    @RequiresPermissions("ota:fota:task:list")
    @Override
    @GetMapping(value = "/list")
    public TableDataInfo list(TaskMpt task) {
        logger.info("管理后台用户[{}]分页查询升级任务", SecurityUtils.getUsername());
        startPage();
        List<TaskPo> taskPoList = taskAppService.search(task.getName(), getBeginTime(task), getEndTime(task));
        List<TaskMpt> taskMptList = TaskMptAssembler.INSTANCE.fromPoList(taskPoList);
        return getDataTable(taskPoList, taskMptList);
    }

    /**
     * 获取所有升级任务状态
     *
     * @return 升级任务状态列表
     */
    @RequiresPermissions("ota:fota:task:list")
    @Override
    @GetMapping(value = "/listAllTaskState")
    public AjaxResult listAllTaskState() {
        logger.info("管理后台用户[{}]获取所有升级任务状态", SecurityUtils.getUsername());
        List<Map<String, Object>> list = new ArrayList<>();
        for (TaskState taskState : TaskState.values()) {
            list.add(Map.of("value", taskState.value, "label", taskState.label));
        }
        return success(list);
    }

    /**
     * 导出升级任务
     *
     * @param response 响应
     * @param task     升级任务
     */
    @Log(title = "升级任务管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:task:export")
    @Override
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaskMpt task) {
        logger.info("管理后台用户[{}]导出升级任务", SecurityUtils.getUsername());
    }

    /**
     * 根据升级任务ID获取升级任务
     *
     * @param taskId 升级任务ID
     * @return 升级任务
     */
    @RequiresPermissions("ota:fota:task:query")
    @Override
    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId) {
        logger.info("管理后台用户[{}]根据升级任务ID[{}]获取升级任务", SecurityUtils.getUsername(), taskId);
        TaskPo taskPo = taskAppService.getTaskById(taskId);
        return success(TaskMptAssembler.INSTANCE.fromPo(taskPo));
    }

    /**
     * 新增升级任务
     *
     * @param task 升级任务
     * @return 结果
     */
    @Log(title = "升级任务管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:fota:task:add")
    @Override
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TaskMpt task) {
        logger.info("管理后台用户[{}]新增升级任务[{}]", SecurityUtils.getUsername(), task.getName());
        TaskPo taskPo = TaskMptAssembler.INSTANCE.toPo(task);
        taskPo.setCreateBy(SecurityUtils.getUserId().toString());
        return toAjax(taskAppService.createTask(taskPo, assemblerTaskRestrictions(task), assemblerTaskStrategies(task)));
    }

    /**
     * 修改保存升级任务
     *
     * @param task 升级任务
     * @return 结果
     */
    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:edit")
    @Override
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TaskMpt task) {
        logger.info("管理后台用户[{}]修改保存升级任务[{}]", SecurityUtils.getUsername(), task.getName());
        TaskPo taskPo = TaskMptAssembler.INSTANCE.toPo(task);
        taskPo.setModifyBy(SecurityUtils.getUserId().toString());
        TaskDo taskDo = taskRepository.getById(taskPo.getId()).orElseThrow(() -> new TaskNotExistException(taskPo.getId()));
        taskDo.edit(taskPo, assemblerTaskRestrictions(task), assemblerTaskStrategies(task));
        taskRepository.save(taskDo);
        return toAjax(1);
    }

    /**
     * 提交升级任务
     *
     * @param taskId 升级任务ID
     * @param task   升级任务
     * @return 结果
     */
    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:submit")
    @Override
    @PostMapping("/{taskId}/action/submit")
    public AjaxResult submit(@PathVariable Long taskId, @Validated @RequestBody TaskMpt task) {
        logger.info("管理后台用户[{}]提交升级任务[{}]", SecurityUtils.getUsername(), taskId);
        if (task == null) {
            task = TaskMpt.builder().build();
        }
        task.setId(taskId);
        TaskPo taskPo = TaskMptAssembler.INSTANCE.toPo(task);
        taskPo.setModifyBy(SecurityUtils.getUserId().toString());
        TaskDo taskDo = taskRepository.getById(taskPo.getId()).orElseThrow(() -> new TaskNotExistException(taskPo.getId()));
        int result = taskDo.submit(taskPo, assemblerTaskRestrictions(task), assemblerTaskStrategies(task));
        taskRepository.save(taskDo);
        return toAjax(result);
    }

    /**
     * 审核升级任务
     *
     * @param taskId    升级任务ID
     * @param taskAudit 升级任务审核
     * @return 结果
     */
    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:audit")
    @Override
    @PostMapping("/{taskId}/action/audit")
    public AjaxResult audit(@PathVariable Long taskId, @Validated @RequestBody TaskAuditMpt taskAudit) {
        logger.info("管理后台用户[{}]审核升级任务[{}]", SecurityUtils.getUsername(), taskId);
        TaskDo taskDo = taskRepository.getById(taskId).orElseThrow(() -> new TaskNotExistException(taskId));
        int result = taskDo.audit(taskAudit.getAudit(), taskAudit.getReason());
        taskRepository.save(taskDo);
        return toAjax(result);
    }

    /**
     * 发布升级任务
     *
     * @param taskId 升级任务ID
     * @return 结果
     */
    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:release")
    @Override
    @PostMapping("/{taskId}/action/release")
    public AjaxResult release(@PathVariable Long taskId) {
        logger.info("管理后台用户[{}]发布升级任务[{}]", SecurityUtils.getUsername(), taskId);
        TaskDo taskDo = taskRepository.getById(taskId).orElseThrow(() -> new TaskNotExistException(taskId));
        int result = taskDo.release();
        taskRepository.save(taskDo);
        cacheService.addReleaseTask(taskDo);
        return toAjax(result);
    }

    /**
     * 暂停升级任务
     *
     * @param taskId 升级任务ID
     * @return 结果
     */
    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:pause")
    @Override
    @PostMapping("/{taskId}/action/pause")
    public AjaxResult pause(@PathVariable Long taskId) {
        logger.info("管理后台用户[{}]暂停升级任务[{}]", SecurityUtils.getUsername(), taskId);
        TaskDo taskDo = taskRepository.getById(taskId).orElseThrow(() -> new TaskNotExistException(taskId));
        int result = taskDo.pause();
        taskRepository.save(taskDo);
        cacheService.removeReleaseTask(taskDo);
        return toAjax(result);
    }

    /**
     * 恢复升级任务
     *
     * @param taskId 升级任务ID
     * @return 结果
     */
    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:resume")
    @Override
    @PostMapping("/{taskId}/action/resume")
    public AjaxResult resume(@PathVariable Long taskId) {
        logger.info("管理后台用户[{}]恢复升级任务[{}]", SecurityUtils.getUsername(), taskId);
        TaskDo taskDo = taskRepository.getById(taskId).orElseThrow(() -> new TaskNotExistException(taskId));
        int result = taskDo.resume();
        taskRepository.save(taskDo);
        cacheService.addReleaseTask(taskDo);
        return toAjax(result);
    }

    /**
     * 取消升级任务
     *
     * @param taskId 升级任务ID
     * @return 结果
     */
    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:cancel")
    @Override
    @PostMapping("/{taskId}/action/cancel")
    public AjaxResult cancel(@PathVariable Long taskId) {
        logger.info("管理后台用户[{}]取消升级任务[{}]", SecurityUtils.getUsername(), taskId);
        TaskDo taskDo = taskRepository.getById(taskId).orElseThrow(() -> new TaskNotExistException(taskId));
        int result = taskDo.cancel();
        taskRepository.save(taskDo);
        cacheService.removeReleaseTask(taskDo);
        return toAjax(result);
    }

    /**
     * 删除升级任务
     *
     * @param taskIds 升级任务ID数组
     * @return 结果
     */
    @Log(title = "升级任务管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:task:remove")
    @Override
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds) {
        logger.info("管理后台用户[{}]删除升级任务[{}]", SecurityUtils.getUsername(), taskIds);
        return toAjax(taskAppService.deleteTaskByIds(taskIds));
    }

    /**
     * 组装任务限制
     *
     * @param task 升级任务
     * @return 任务限制
     */
    private List<TaskRestrictionPo> assemblerTaskRestrictions(TaskMpt task) {
        List<TaskRestrictionPo> taskRestrictionList = new ArrayList<>();
        if (task.getAdaptiveSubject() != null) {
            taskRestrictionList.add(TaskRestrictionPo.builder()
                    .restrictionType(ADAPTATION_SUBJECT.name())
                    .restrictionExpression(String.valueOf(task.getAdaptiveSubject()))
                    .build());
        }
        if (StrUtil.isNotBlank(task.getExcludedBaseline())) {
            taskRestrictionList.add(TaskRestrictionPo.builder()
                    .restrictionType(BASELINE_EXCLUDE.name())
                    .restrictionExpression(task.getExcludedBaseline())
                    .build());
        }
        if (task.getBaselineUnification() != null) {
            taskRestrictionList.add(TaskRestrictionPo.builder()
                    .restrictionType(BASELINE_UNIFICATION.name())
                    .restrictionExpression(String.valueOf(task.getBaselineUnification()))
                    .build());
        }
        if (task.getComparisonCriteria() != null) {
            taskRestrictionList.add(TaskRestrictionPo.builder()
                    .restrictionType(COMPARISON_CRITERIA.name())
                    .restrictionExpression(String.valueOf(task.getComparisonCriteria()))
                    .build());
        }
        return taskRestrictionList;
    }

    /**
     * 组装任务策略
     *
     * @param task 升级任务
     * @return 任务策略
     */
    private List<TaskStrategyPo> assemblerTaskStrategies(TaskMpt task) {
        List<TaskStrategyPo> taskStrategyList = new ArrayList<>();
        if (task.getRollback() != null) {
            taskStrategyList.add(TaskStrategyPo.builder()
                    .strategyType(ROLLBACK.name())
                    .strategyExpression(String.valueOf(task.getRollback()))
                    .build());
        }
        if (task.getFlashCount() != null) {
            taskStrategyList.add(TaskStrategyPo.builder()
                    .strategyType(FLASH_COUNT.name())
                    .strategyExpression(String.valueOf(task.getFlashCount()))
                    .build());
        }
        if (task.getImpactVehicleOperation() != null) {
            taskStrategyList.add(TaskStrategyPo.builder()
                    .strategyType(IMPACT_VEHICLE_OPERATION.name())
                    .strategyExpression(String.valueOf(task.getImpactVehicleOperation()))
                    .build());
        }
        if (task.getKeepInPark() != null) {
            taskStrategyList.add(TaskStrategyPo.builder()
                    .strategyType(KEEP_IN_PARK.name())
                    .strategyExpression(String.valueOf(task.getKeepInPark()))
                    .build());
        }
        if (task.getNotCharging() != null) {
            taskStrategyList.add(TaskStrategyPo.builder()
                    .strategyType(NOT_CHARGING.name())
                    .strategyExpression(String.valueOf(task.getNotCharging()))
                    .build());
        }
        if (task.getNoExternalPower() != null) {
            taskStrategyList.add(TaskStrategyPo.builder()
                    .strategyType(NO_EXTERNAL_POWER.name())
                    .strategyExpression(String.valueOf(task.getNoExternalPower()))
                    .build());
        }
        if (task.getAllClosed() != null) {
            taskStrategyList.add(TaskStrategyPo.builder()
                    .strategyType(ALL_CLOSED.name())
                    .strategyExpression(String.valueOf(task.getAllClosed()))
                    .build());
        }
        if (task.getHvSoc() != null) {
            taskStrategyList.add(TaskStrategyPo.builder()
                    .strategyType(HV_SOC.name())
                    .strategyExpression(String.valueOf(task.getHvSoc()))
                    .build());
        }
        if (task.getLvSoc() != null) {
            taskStrategyList.add(TaskStrategyPo.builder()
                    .strategyType(LV_SOC.name())
                    .strategyExpression(String.valueOf(task.getLvSoc()))
                    .build());
        }
        return taskStrategyList;
    }
}
