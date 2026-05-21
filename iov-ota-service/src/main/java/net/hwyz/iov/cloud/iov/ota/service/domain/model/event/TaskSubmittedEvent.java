package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;

@Getter
public class TaskSubmittedEvent extends TaskEvent {
    
    private final String taskName;
    private final ActivityId activityId;
    
    public TaskSubmittedEvent(TaskId taskId, String taskName, ActivityId activityId) {
        super(taskId);
        this.taskName = taskName;
        this.activityId = activityId;
    }
}