package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.query.TaskQuery;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskResult;
import net.hwyz.iov.cloud.iov.ota.service.application.assembler.TaskAssembler;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskInstallConditionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.event.publisher.DomainEventPublisher;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.TaskNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TargetResolutionDomainService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ApprovalDomainService;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ApprovalLevel;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskApproval;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAppService {

    private final TaskRepository taskRepository;
    private final TaskInstallConditionRepository taskInstallConditionRepository;
    private final TaskAssembler taskAssembler;
    private final DomainEventPublisher eventPublisher;
    private final ActivityAppService activityAppService;
    private final ApprovalDomainService approvalDomainService;
    private final TargetResolutionDomainService targetResolutionDomainService;

    public List<TaskResult> search(String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return taskRepository.findReleasedTasks().stream()
            .filter(task -> {
                if (name != null && !task.getName().contains(name)) return false;
                if (beginTime != null && task.getStartTime().isBefore(beginTime.toInstant())) return false;
                if (endTime != null && task.getEndTime().isAfter(endTime.toInstant())) return false;
                return true;
            })
            .map(taskAssembler::toResult)
            .collect(Collectors.toList());
    }

    public TaskResult getTaskById(Long id) {
        Task task = taskRepository.getById(TaskId.of(id))
            .orElseThrow(() -> new TaskNotExistException(id));
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult createTask(TaskCreateCmd cmd) {
        log.info("创建任务: {}", cmd.getName());

        validateTaskWindowWithinActivity(cmd.getActivityId(), cmd.getStartTime(), cmd.getEndTime());

        // US-061: 废弃 TaskType.LIGHT，type 默认为 NORMAL
        TaskType taskType = (cmd.getType() != null && !cmd.getType().isEmpty()) 
            ? TaskType.valOf(Integer.parseInt(cmd.getType())) : TaskType.NORMAL;
        
        Task task = Task.create(
            TaskId.of(generateId()),
            cmd.getName(),
            taskType,
            ActivityId.of(cmd.getActivityId())
        );
        task.setTarget(cmd.getTarget());
        task.setStartTime(cmd.getStartTime());
        task.setEndTime(cmd.getEndTime());
        task.setNoticeType(cmd.getNoticeType());
        task.setUpgradeMode(cmd.getUpgradeMode() != null && !cmd.getUpgradeMode().isEmpty() 
            ? net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeMode.valOf(Integer.parseInt(cmd.getUpgradeMode())) : null);
        task.setDescription(cmd.getDescription());
        
        if (cmd.getRestrictions() != null) {
            task.loadRestrictionsAndStrategies(
                taskAssembler.toRestrictions(cmd.getRestrictions()),
                taskAssembler.toStrategies(cmd.getStrategies())
            );
        }

        taskRepository.save(task);

        // 保存安装条件
        if (cmd.getInstallConditions() != null && !cmd.getInstallConditions().isEmpty()) {
            List<net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition> installConditions =
                taskAssembler.toInstallConditions(cmd.getInstallConditions());
            for (net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition condition : installConditions) {
                condition.setId(null);
                condition.setTaskId(task.getId().getValue());
                taskInstallConditionRepository.save(condition);
            }
        }

        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult submitTask(TaskSubmitCmd cmd) {
        log.info("提交任务: {}", cmd.getTaskId());

        Task task = taskRepository.getById(TaskId.of(cmd.getTaskId()))
            .orElseThrow(() -> new TaskNotExistException(cmd.getTaskId()));

        taskAssembler.updateFromCmd(task, cmd);
        task.submit();

        // 检查阶段是否需要审批，如果不需要则直接通过
        if (approvalDomainService.checkApprovalRequirements(
            cmd.getTaskId(), task.getPhase(), task.getActivityId().getValue())) {
            task.approve(true, null);
            log.info("任务[{}]阶段[{}]不需要审批，直接通过", cmd.getTaskId(), task.getPhase());
        }

        taskRepository.save(task);

        // 更新安装条件
        if (cmd.getInstallConditions() != null && !cmd.getInstallConditions().isEmpty()) {
            // 删除原有安装条件
            taskInstallConditionRepository.deleteByTaskId(cmd.getTaskId());
            // 保存新的安装条件
            List<net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition> installConditions =
                taskAssembler.toInstallConditions(cmd.getInstallConditions());
            for (net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition condition : installConditions) {
                condition.setId(null);
                condition.setTaskId(cmd.getTaskId());
                taskInstallConditionRepository.save(condition);
            }
        }

        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();

        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult auditTask(TaskAuditCmd cmd) {
        log.info("审批任务: {}, 级别: {}, 结果: {}", cmd.getTaskId(), cmd.getApprovalLevel(), cmd.getResult());
        
        Task task = taskRepository.getById(TaskId.of(cmd.getTaskId()))
            .orElseThrow(() -> new TaskNotExistException(cmd.getTaskId()));
        
        // 提交审批记录
        ApprovalLevel level = ApprovalLevel.valueOf(cmd.getApprovalLevel());
        TaskApproval approval = approvalDomainService.submitApproval(
            cmd.getTaskId(), 
            level, 
            SecurityUtils.getUserId().toString(), 
            cmd.getResult(), 
            cmd.getComment()
        );
        
        // 根据审批结果更新任务状态
        if ("REJECTED".equals(cmd.getResult())) {
            // 任一级别拒绝，任务状态变为REJECTED
            task.approve(false, cmd.getComment());
        } else if ("APPROVED".equals(cmd.getResult())) {
            // 检查是否所有级别都已审批通过
            boolean allApproved = approvalDomainService.checkApprovalRequirements(
                cmd.getTaskId(), 
                task.getPhase(), 
                task.getActivityId().getValue()
            );
            
            if (allApproved) {
                // 所有级别都已通过，任务状态变为APPROVED
                task.approve(true, null);
            } else {
                // 还有其他级别需要审批，任务状态保持PENDING_APPROVAL
                log.info("任务[{}]在[{}]级别审批通过，等待后续审批", cmd.getTaskId(), cmd.getApprovalLevel());
            }
        }
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    /**
     * 统一发布任务（立即发布）
     * 状态CAS、车辆快照、条件快照、状态日志和Outbox在同一数据库事务内提交
     */
    @Transactional
    public TaskResult releaseTask(Long taskId) {
        log.info("立即发布任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        // US-060: 审批与阶段结合 - 发布前检查审批要求
        if (!approvalDomainService.checkApprovalRequirements(taskId, task.getPhase(), task.getActivityId().getValue())) {
            throw new IllegalStateException("任务[" + taskId + "]未满足阶段[" + task.getPhase() + "]的审批要求");
        }
        
        // 解析目标定义，获取车辆集合
        Set<Vin> vehicles = targetResolutionDomainService.resolveTarget(task.getTarget());
        
        // 统一发布事务：IMMEDIATE触发方式
        task.release(vehicles, "IMMEDIATE");
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }
    
    /**
     * 统一发布任务（到点发布，由调度器触发）
     */
    @Transactional
    public TaskResult releaseTaskByScheduler(Long taskId) {
        log.info("到点发布任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        // 解析目标定义，获取车辆集合
        Set<Vin> vehicles = targetResolutionDomainService.resolveTarget(task.getTarget());
        
        // 统一发布事务：SCHEDULER触发方式
        task.release(vehicles, "SCHEDULER");
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult pauseTask(Long taskId) {
        log.info("暂停任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.pause();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult resumeTask(Long taskId) {
        log.info("恢复任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.resume();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult cancelTask(Long taskId) {
        log.info("取消任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.cancel();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    /**
     * 排程任务（定时发布）
     * @param taskId 任务ID
     * @param releaseTime 计划发布时间
     */
    @Transactional
    public TaskResult scheduleTask(Long taskId, Instant releaseTime) {
        log.info("排程任务: {}, 计划发布时间: {}", taskId, releaseTime);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.schedule(releaseTime);
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }
    
    /**
     * 取消排程
     * @param taskId 任务ID
     */
    @Transactional
    public TaskResult unscheduleTask(Long taskId) {
        log.info("取消排程任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.unschedule();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }
    
    /**
     * 激活放量（首批放量或首车领取）
     * @param taskId 任务ID
     */
    @Transactional
    public TaskResult activateRollout(Long taskId) {
        log.info("激活放量任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.activateRollout();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult pauseTaskWithReason(Long taskId, String pauseReason, String pausedBy) {
        log.info("暂停任务: {}, 原因: {}, 发起方: {}", taskId, pauseReason, pausedBy);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.pause(pauseReason, pausedBy);
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult cancelTaskWithReason(Long taskId, String cancelReason) {
        log.info("取消任务: {}, 原因: {}", taskId, cancelReason);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.cancel(cancelReason);
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult supersedeTask(Long taskId) {
        log.info("取代任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.supersede();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult finishTask(Long taskId) {
        log.info("结束任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.finish();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    public int deleteTaskByIds(Long[] ids) {
        List<TaskId> taskIdList = List.of(ids).stream()
            .map(TaskId::of)
            .collect(Collectors.toList());
        taskRepository.deleteAll(taskIdList);
        return ids.length;
    }

    private Long generateId() {
        return System.currentTimeMillis();
    }

    /**
     * 校验任务时间窗完全落在活动计划窗口内
     *
     * @param activityId 活动ID
     * @param taskStart  任务开始时间
     * @param taskEnd    任务结束时间
     */
    private void validateTaskWindowWithinActivity(Long activityId, java.time.Instant taskStart, java.time.Instant taskEnd) {
        if (activityId == null || taskStart == null || taskEnd == null) {
            return;
        }
        net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo activity =
                activityAppService.getActivityById(activityId);
        if (activity == null) {
            return;
        }
        if (activity.getStartTime() != null && taskStart.isBefore(activity.getStartTime().toInstant())) {
            throw new IllegalStateException("任务开始时间不能早于活动计划窗口开始时间");
        }
        if (activity.getEndTime() != null && taskEnd.isAfter(activity.getEndTime().toInstant())) {
            throw new IllegalStateException("任务结束时间不能晚于活动计划窗口结束时间");
        }
    }
}