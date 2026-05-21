package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;

@Getter
public class TaskRejectedEvent extends TaskEvent {
    
    private final String taskName;
    private final String reason;
    
    public TaskRejectedEvent(TaskId taskId, String taskName, String reason) {
        super(taskId);
        this.taskName = taskName;
        this.reason = reason;
    }
}