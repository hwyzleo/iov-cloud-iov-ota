package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;

import java.time.Instant;

@Getter
public class TaskScheduledEvent extends TaskEvent {
    
    private final String taskName;
    private final Instant releaseTime;
    
    public TaskScheduledEvent(TaskId taskId, String taskName, Instant releaseTime) {
        super(taskId);
        this.taskName = taskName;
        this.releaseTime = releaseTime;
    }
}