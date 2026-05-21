package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import java.time.Instant;

@Getter
public abstract class TaskEvent {
    
    private final Instant occurredOn;
    private final TaskId taskId;
    
    protected TaskEvent(TaskId taskId) {
        this.taskId = taskId;
        this.occurredOn = Instant.now();
    }
}