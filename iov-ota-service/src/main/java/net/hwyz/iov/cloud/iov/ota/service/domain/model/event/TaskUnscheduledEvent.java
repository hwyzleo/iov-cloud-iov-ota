package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;

@Getter
public class TaskUnscheduledEvent extends TaskEvent {
    
    private final String taskName;
    
    public TaskUnscheduledEvent(TaskId taskId, String taskName) {
        super(taskId);
        this.taskName = taskName;
    }
}
