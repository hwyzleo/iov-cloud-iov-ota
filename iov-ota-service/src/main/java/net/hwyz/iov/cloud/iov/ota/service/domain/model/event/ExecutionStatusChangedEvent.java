package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;

/**
 * 安装执行状态变更事件（CR-012 §2.3）
 *
 * @author hwyz_leo
 */
@Getter
public class ExecutionStatusChangedEvent extends ExecutionEvent {

    private final ExecutionStatus previousStatus;
    private final ExecutionStatus currentStatus;

    public ExecutionStatusChangedEvent(ExecutionId executionId, ExecutionStatus previousStatus, ExecutionStatus currentStatus) {
        super(executionId);
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
    }
}
