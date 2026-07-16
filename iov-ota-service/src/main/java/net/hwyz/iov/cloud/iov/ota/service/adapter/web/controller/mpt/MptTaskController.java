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
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskAuditMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskCancelMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskPauseMpt;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.TaskMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ApprovalDomainService;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskApproval;
import net.hwyz.iov.cloud.iov.ota.api.vo.TaskApprovalMpt;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.InstallConditionType;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.InstallConditionTypeRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskInstallConditionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskRestrictionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskStrategyMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskRestrictionPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskStrategyPo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/task/v1")
public class MptTaskController extends BaseController {

    private final CacheService cacheService;
    private final TaskAppService taskAppService;
    private final TaskRepository taskRepository;
    private final TaskRestrictionMapper taskRestrictionMapper;
    private final TaskStrategyMapper taskStrategyMapper;
    private final TaskInstallConditionRepository taskInstallConditionRepository;
    private final InstallConditionTypeRepository installConditionTypeRepository;
    private final TaskMptAssembler taskMptAssembler;
    private final ApprovalDomainService approvalDomainService;

    @RequiresPermissions("ota:fota:task:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<TaskMpt>> list(TaskMpt task) {
        log.info("管理后台用户[{}]分页查询升级任务", SecurityUtils.getUsername());
        startPage();
        List<TaskResult> results = taskAppService.search(task.getName(), task.getStartTime(), task.getEndTime());
        return ApiResponse.ok(getPageResult(PageUtil.convert(results, taskMptAssembler::toVo)));
    }

    @RequiresPermissions("ota:fota:task:list")
    @GetMapping(value = "/listAllTaskState")
    public ApiResponse<List<Map<String, Object>>> listAllTaskState() {
        log.info("管理后台用户[{}]获取所有升级任务状态", SecurityUtils.getUsername());
        List<Map<String, Object>> list = new ArrayList<>();
        for (TaskState taskState : TaskState.values()) {
            list.add(Map.of("value", taskState.value, "label", taskState.label));
        }
        return ApiResponse.ok(list);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:task:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaskMpt task) {
        log.info("管理后台用户[{}]导出升级任务", SecurityUtils.getUsername());
    }

    @RequiresPermissions("ota:fota:task:query")
    @GetMapping(value = "/{taskId}")
    public ApiResponse<TaskMpt> getInfo(@PathVariable Long taskId) {
        log.info("管理后台用户[{}]根据升级任务ID[{}]获取升级任务", SecurityUtils.getUsername(), taskId);
        TaskResult result = taskAppService.getTaskById(taskId);
        List<TaskRestrictionPo> restrictions = taskRestrictionMapper.selectPoByTaskId(taskId);
        List<TaskStrategyPo> strategies = taskStrategyMapper.selectPoByTaskId(taskId);
        List<TaskInstallCondition> installConditions = taskInstallConditionRepository.listByTaskId(taskId);
        Map<String, InstallConditionType> conditionTypeMap = installConditionTypeRepository.listAll()
            .stream()
            .collect(Collectors.toMap(InstallConditionType::getCode, t -> t));
        TaskMpt vo = taskMptAssembler.fromPo(taskMptAssembler.toPo(taskMptAssembler.toVo(result)), restrictions, strategies, installConditions, conditionTypeMap);
        return ApiResponse.ok(vo);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:fota:task:add")
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody TaskMpt task) {
        log.info("管理后台用户[{}]新增升级任务[{}]", SecurityUtils.getUsername(), task.getName());
        TaskCreateCmd cmd = taskMptAssembler.toCmd(task);
        taskAppService.createTask(cmd);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:edit")
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody TaskMpt task) {
        log.info("管理后台用户[{}]修改保存升级任务[{}]", SecurityUtils.getUsername(), task.getName());
        TaskSubmitCmd cmd = taskMptAssembler.toSubmitCmd(task);
        taskAppService.submitTask(cmd);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:submit")
    @PostMapping("/{taskId}/action/submit")
    public ApiResponse<Integer> submit(@PathVariable Long taskId, @Validated @RequestBody TaskMpt task) {
        log.info("管理后台用户[{}]提交升级任务[{}]", SecurityUtils.getUsername(), taskId);
        if (task == null) {
            task = TaskMpt.builder().build();
        }
        task.setId(taskId);
        TaskSubmitCmd cmd = taskMptAssembler.toSubmitCmd(task);
        taskAppService.submitTask(cmd);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:audit")
    @PostMapping("/{taskId}/action/audit")
    public ApiResponse<Integer> audit(@PathVariable Long taskId, @Validated @RequestBody TaskAuditMpt taskAudit) {
        log.info("管理后台用户[{}]审核升级任务[{}]", SecurityUtils.getUsername(), taskId);
        TaskAuditCmd cmd = taskMptAssembler.toAuditCmd(taskAudit);
        cmd.setTaskId(taskId);
        taskAppService.auditTask(cmd);
        return ApiResponse.ok(1);
    }

    /**
     * 查询任务多级审批记录
     *
     * @param taskId 升级任务ID
     * @return 审批记录列表
     */
    @RequiresPermissions("ota:fota:task:list")
    @GetMapping(value = "/{taskId}/listApproval")
    public ApiResponse<List<TaskApprovalMpt>> listApproval(@PathVariable Long taskId) {
        log.info("管理后台用户[{}]查询升级任务[{}]审批记录", SecurityUtils.getUsername(), taskId);
        List<TaskApproval> approvals = approvalDomainService.listApprovals(taskId);
        List<TaskApprovalMpt> result = approvals.stream()
            .map(approval -> TaskApprovalMpt.builder()
                .id(approval.getId())
                .taskId(approval.getTaskId())
                .level(approval.getLevel().name())
                .approver(approval.getApprover())
                .result(approval.getResult())
                .comment(approval.getComment())
                .decidedAt(approval.getDecidedAt())
                .approvalRef(approval.getApprovalRef())
                .build())
            .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:release")
    @PostMapping("/{taskId}/action/release")
    public ApiResponse<Integer> release(@PathVariable Long taskId) {
        log.info("管理后台用户[{}]发布升级任务[{}]", SecurityUtils.getUsername(), taskId);
        taskAppService.releaseTask(taskId);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:schedule")
    @PostMapping("/{taskId}/action/schedule")
    public ApiResponse<Integer> schedule(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        log.info("管理后台用户[{}]排程升级任务[{}]", SecurityUtils.getUsername(), taskId);
        String releaseTimeStr = (String) body.get("releaseTime");
        Instant releaseTime;
        try {
            releaseTime = Instant.parse(releaseTimeStr);
        } catch (DateTimeParseException e) {
            releaseTime = LocalDateTime.parse(releaseTimeStr)
                .atZone(ZoneId.of("Asia/Shanghai"))
                .toInstant();
        }
        taskAppService.scheduleTask(taskId, releaseTime);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:unschedule")
    @PostMapping("/{taskId}/action/unschedule")
    public ApiResponse<Integer> unschedule(@PathVariable Long taskId) {
        log.info("管理后台用户[{}]取消排程升级任务[{}]", SecurityUtils.getUsername(), taskId);
        taskAppService.unscheduleTask(taskId);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:pause")
    @PostMapping("/{taskId}/action/pause")
    public ApiResponse<Integer> pause(@PathVariable Long taskId) {
        log.info("管理后台用户[{}]暂停升级任务[{}]", SecurityUtils.getUsername(), taskId);
        taskAppService.pauseTask(taskId);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:pause")
    @PostMapping("/{taskId}/action/pauseWithReason")
    public ApiResponse<Integer> pauseWithReason(@PathVariable Long taskId, @RequestBody TaskPauseMpt taskPause) {
        log.info("管理后台用户[{}]暂停升级任务[{}]，原因[{}]，发起方[{}]", SecurityUtils.getUsername(), taskId, taskPause.getPauseReason(), taskPause.getPausedBy());
        taskAppService.pauseTaskWithReason(taskId, taskPause.getPauseReason(), taskPause.getPausedBy());
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:resume")
    @PostMapping("/{taskId}/action/resume")
    public ApiResponse<Integer> resume(@PathVariable Long taskId) {
        log.info("管理后台用户[{}]恢复升级任务[{}]", SecurityUtils.getUsername(), taskId);
        taskAppService.resumeTask(taskId);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:cancel")
    @PostMapping("/{taskId}/action/cancel")
    public ApiResponse<Integer> cancel(@PathVariable Long taskId) {
        log.info("管理后台用户[{}]取消升级任务[{}]", SecurityUtils.getUsername(), taskId);
        taskAppService.cancelTask(taskId);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:cancel")
    @PostMapping("/{taskId}/action/cancelWithReason")
    public ApiResponse<Integer> cancelWithReason(@PathVariable Long taskId, @RequestBody TaskCancelMpt taskCancel) {
        log.info("管理后台用户[{}]取消升级任务[{}]，原因[{}]", SecurityUtils.getUsername(), taskId, taskCancel.getCancelReason());
        taskAppService.cancelTaskWithReason(taskId, taskCancel.getCancelReason());
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:supersede")
    @PostMapping("/{taskId}/action/supersede")
    public ApiResponse<Integer> supersede(@PathVariable Long taskId) {
        log.info("管理后台用户[{}]取代升级任务[{}]", SecurityUtils.getUsername(), taskId);
        taskAppService.supersedeTask(taskId);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:task:finish")
    @PostMapping("/{taskId}/action/finish")
    public ApiResponse<Integer> finish(@PathVariable Long taskId) {
        log.info("管理后台用户[{}]结束升级任务[{}]", SecurityUtils.getUsername(), taskId);
        taskAppService.finishTask(taskId);
        return ApiResponse.ok(1);
    }

    @Log(title = "升级任务管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:task:remove")
    @DeleteMapping("/{taskIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] taskIds) {
        log.info("管理后台用户[{}]删除升级任务[{}]", SecurityUtils.getUsername(), taskIds);
        return ApiResponse.ok(taskAppService.deleteTaskByIds(taskIds));
    }
}