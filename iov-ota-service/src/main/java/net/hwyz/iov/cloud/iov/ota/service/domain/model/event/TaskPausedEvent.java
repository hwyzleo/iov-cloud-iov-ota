package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;

public class TaskPausedEvent extends TaskEvent {
    
    public TaskPausedEvent(TaskId taskId) {
        super(taskId);
    }
}