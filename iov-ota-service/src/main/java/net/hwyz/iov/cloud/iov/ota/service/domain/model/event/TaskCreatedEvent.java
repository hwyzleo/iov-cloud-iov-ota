package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;

@Getter
public class TaskCreatedEvent extends TaskEvent {
    
    private final String taskName;
    private final TaskType taskType;
    
    public TaskCreatedEvent(TaskId taskId, String taskName, TaskType taskType) {
        super(taskId);
        this.taskName = taskName;
        this.taskType = taskType;
    }
}