package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 任务状态迁移审计（CR-015 §3.4 stateLogs）
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TaskStateLogResult {

    private Long taskId;
    private String fromState;
    private String toState;
    private String action;
    private String operator;
    private String reason;
    private Instant decidedAt;
}
