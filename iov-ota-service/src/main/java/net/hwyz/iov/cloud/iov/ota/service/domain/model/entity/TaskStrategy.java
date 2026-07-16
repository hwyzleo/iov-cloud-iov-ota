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
    private final Integer downloadRetryMax;  // 下载重试最大次数
    private final String retryBackoff;  // 重试退避策略：FIXED/EXP
    private final Boolean resumeOnPoweroff;  // 断电后是否续传
}