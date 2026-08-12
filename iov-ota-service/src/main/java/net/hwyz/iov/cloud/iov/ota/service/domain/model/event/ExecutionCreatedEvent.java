package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;

/**
 * 安装执行已创建事件（CR-012 §5.5）
 *
 * <p>Execution 创建、attemptNo 递增与 activeExecution 唯一性在同一事务内提交。
 *
 * @author hwyz_leo
 */
@Getter
public class ExecutionCreatedEvent extends ExecutionEvent {

    private final VehicleTaskId vehicleTaskId;
    private final int attemptNo;
    private final String permitToken;

    public ExecutionCreatedEvent(ExecutionId executionId, VehicleTaskId vehicleTaskId, int attemptNo, String permitToken) {
        super(executionId);
        this.vehicleTaskId = vehicleTaskId;
        this.attemptNo = attemptNo;
        this.permitToken = permitToken;
    }
}
