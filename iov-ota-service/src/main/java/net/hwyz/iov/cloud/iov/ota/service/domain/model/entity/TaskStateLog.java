package net.hwyz.iov.cloud.iov.ota.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 任务状态迁移审计领域实体
 * 对应表：tb_task_state_log
 */
@Getter
@Setter
@Builder
public class TaskStateLog {
    
    private Long id;
    private Long taskId;
    private Integer fromState;  // 原状态
    private Integer toState;  // 新状态
    private String action;  // 操作：SUBMIT/AUDIT/RELEASE/PAUSE/RESUME/CANCEL/FINISH/SUPERSEDE
    private String operator;  // 操作人
    private String reason;  // 原因
    private Instant decidedAt;  // 决策时间
    private String description;  // 备注
    
    /**
     * 创建状态迁移日志
     *
     * @param taskId 任务ID
     * @param fromState 原状态
     * @param toState 新状态
     * @param action 操作
     * @param operator 操作人
     * @param reason 原因
     * @return 状态迁移日志
     */
    public static TaskStateLog create(Long taskId, Integer fromState, Integer toState, 
                                       String action, String operator, String reason) {
        return TaskStateLog.builder()
                .taskId(taskId)
                .fromState(fromState)
                .toState(toState)
                .action(action)
                .operator(operator)
                .reason(reason)
                .decidedAt(Instant.now())
                .build();
    }
}
