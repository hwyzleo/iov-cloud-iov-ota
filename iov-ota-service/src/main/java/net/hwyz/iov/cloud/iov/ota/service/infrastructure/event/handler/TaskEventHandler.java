package net.hwyz.iov.cloud.iov.ota.service.infrastructure.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.*;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.PhaseAdvanceDomainService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventHandler {

    private final TaskVehicleRepository taskVehicleRepository;
    private final TaskRepository taskRepository;
    private final CacheService cacheService;
    private final PhaseAdvanceDomainService phaseAdvanceDomainService;

    @EventListener
    public void handleTaskCreated(TaskCreatedEvent event) {
        log.info("任务[{}]创建事件：类型[{}]", event.getTaskId().getValue(), event.getTaskType());
    }

    @EventListener
    public void handleTaskSubmitted(TaskSubmittedEvent event) {
        log.info("任务[{}]提交事件：活动[{}]", event.getTaskId().getValue(), event.getActivityId().getValue());
    }

    @EventListener
    public void handleTaskApproved(TaskApprovedEvent event) {
        log.info("任务[{}]审核通过事件", event.getTaskId().getValue());
    }

    @EventListener
    public void handleTaskRejected(TaskRejectedEvent event) {
        log.info("任务[{}]审核拒绝事件：原因[{}]", event.getTaskId().getValue(), event.getReason());
    }

    @EventListener
    public void handleTaskReleased(TaskReleasedEvent event) {
        log.info("任务[{}]发布事件：影响车辆数量[{}]", event.getTaskId().getValue(), event.getAffectedVehicles().size());
        Long taskId = event.getTaskId().getValue();
        Optional<Task> taskOpt = taskRepository.getById(TaskId.of(taskId));
        if (taskOpt.isEmpty()) {
            log.warn("任务[{}]不存在，跳过发布处理", taskId);
            return;
        }
        Task task = taskOpt.get();
        int created = taskVehicleRepository.batchCreate(taskId, task.getActivityId().getValue(), event.getAffectedVehicles());
        log.info("任务[{}]批量创建升级任务车辆记录：新增[{}]，总数[{}]", taskId, created, event.getAffectedVehicles().size());
        cacheService.addReleaseTask(task);
        log.info("任务[{}]已写入缓存", taskId);
    }

    @EventListener
    public void handleTaskPaused(TaskPausedEvent event) {
        log.info("任务[{}]暂停事件", event.getTaskId().getValue());
        Long taskId = event.getTaskId().getValue();
        taskRepository.getById(TaskId.of(taskId)).ifPresent(task -> {
            cacheService.removeReleaseTask(task);
            log.info("任务[{}]已从缓存移除", taskId);
        });
    }

    @EventListener
    public void handleTaskResumed(TaskResumedEvent event) {
        log.info("任务[{}]恢复事件", event.getTaskId().getValue());
        Long taskId = event.getTaskId().getValue();
        taskRepository.getById(TaskId.of(taskId)).ifPresent(task -> {
            cacheService.addReleaseTask(task);
            log.info("任务[{}]已重新写入缓存", taskId);
        });
    }

    @EventListener
    public void handleTaskCancelled(TaskCancelledEvent event) {
        log.info("任务[{}]取消事件", event.getTaskId().getValue());
        Long taskId = event.getTaskId().getValue();
        taskRepository.getById(TaskId.of(taskId)).ifPresent(task -> {
            cacheService.removeReleaseTask(task);
            log.info("任务[{}]已从缓存移除", taskId);
        });
    }

    @EventListener
    public void handleTaskScheduled(TaskScheduledEvent event) {
        log.info("任务[{}]排程事件：计划发布时间[{}]", event.getTaskId().getValue(), event.getReleaseTime());
        // 排程任务不需要写入缓存，等待到点发布
    }

    @EventListener
    public void handleTaskUnscheduled(TaskUnscheduledEvent event) {
        log.info("任务[{}]取消排程事件", event.getTaskId().getValue());
        // 取消排程不需要操作缓存
    }

    @EventListener
    public void handleTaskActivated(TaskActivatedEvent event) {
        log.info("任务[{}]激活放量事件", event.getTaskId().getValue());
        Long taskId = event.getTaskId().getValue();
        taskRepository.getById(TaskId.of(taskId)).ifPresent(task -> {
            cacheService.addReleaseTask(task);
            log.info("任务[{}]激活放量后已写入缓存", taskId);
        });
    }

    @EventListener
    public void handleTaskFinished(TaskFinishedEvent event) {
        log.info("任务[{}]结束事件", event.getTaskId().getValue());
        Long taskId = event.getTaskId().getValue();
        taskRepository.getById(TaskId.of(taskId)).ifPresent(task -> {
            cacheService.removeReleaseTask(task);
            log.info("任务[{}]已从缓存移除（FINISHED）", taskId);
            
            // 尝试推进到下一阶段（US-054）
            try {
                boolean advanced = phaseAdvanceDomainService.tryAdvanceToNextPhase(task);
                if (advanced) {
                    log.info("任务[{}]完成后成功推进到下一阶段", taskId);
                } else {
                    log.info("任务[{}]完成后未推进到下一阶段", taskId);
                }
            } catch (Exception e) {
                log.error("任务[{}]推进阶段时发生错误", taskId, e);
            }
        });
    }
}
