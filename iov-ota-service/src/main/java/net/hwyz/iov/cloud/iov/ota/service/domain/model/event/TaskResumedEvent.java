package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;

public class TaskResumedEvent extends TaskEvent {
    
    public TaskResumedEvent(TaskId taskId) {
        super(taskId);
    }
}