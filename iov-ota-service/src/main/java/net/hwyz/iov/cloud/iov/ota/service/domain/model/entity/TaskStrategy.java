package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskStrategyType;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;

@Getter
@Builder
public class TaskStrategy {
    
    private final Long id;
    private final TaskId taskId;
    private final TaskStrategyType type;
    private final String strategy;
}