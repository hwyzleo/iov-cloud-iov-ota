package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import java.util.Set;

@Getter
public class TaskReleasedEvent extends TaskEvent {
    
    private final String taskName;
    private final Set<Vin> affectedVehicles;
    
    public TaskReleasedEvent(TaskId taskId, String taskName, Set<Vin> affectedVehicles) {
        super(taskId);
        this.taskName = taskName;
        this.affectedVehicles = affectedVehicles;
    }
}