package net.hwyz.iov.cloud.iov.ota.service.domain.model.event;

import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;

import java.time.Instant;

/**
 * 安装执行领域事件基类（CR-012 §2.3）
 *
 * @author hwyz_leo
 */
@Getter
public abstract class ExecutionEvent {

    private final Instant occurredOn;
    private final ExecutionId executionId;

    protected ExecutionEvent(ExecutionId executionId) {
        this.executionId = executionId;
        this.occurredOn = Instant.now();
    }
}
