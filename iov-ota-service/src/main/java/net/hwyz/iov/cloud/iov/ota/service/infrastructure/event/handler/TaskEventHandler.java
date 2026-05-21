package net.hwyz.iov.cloud.iov.ota.service.infrastructure.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventHandler {

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
    }

    @EventListener
    public void handleTaskCancelled(TaskCancelledEvent event) {
        log.info("任务[{}]取消事件", event.getTaskId().getValue());
    }
}