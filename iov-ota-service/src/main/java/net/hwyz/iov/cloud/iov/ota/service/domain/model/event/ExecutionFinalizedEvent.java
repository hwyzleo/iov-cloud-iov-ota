package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;

/**
 * 安装执行收口事件（CR-012 §5.7）
 *
 * <p>Execution 收口与 VehicleTask 收口是两个条件不同的状态转换。
 *
 * @author hwyz_leo
 */
@Getter
public class ExecutionFinalizedEvent extends ExecutionEvent {

    private final ExecutionStatus finalStatus;
    private final long acceptedSequenceNo;

    public ExecutionFinalizedEvent(ExecutionId executionId, ExecutionStatus finalStatus, long acceptedSequenceNo) {
        super(executionId);
        this.finalStatus = finalStatus;
        this.acceptedSequenceNo = acceptedSequenceNo;
    }
}
