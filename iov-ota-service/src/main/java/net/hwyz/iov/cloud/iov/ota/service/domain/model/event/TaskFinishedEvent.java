package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;

/**
 * 任务结束事件
 *
 * @author hwyz_leo
 */
@Getter
public class TaskFinishedEvent extends TaskEvent {

    private final String taskName;

    public TaskFinishedEvent(TaskId taskId, String taskName) {
        super(taskId);
        this.taskName = taskName;
    }
}
