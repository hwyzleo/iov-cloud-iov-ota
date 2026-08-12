package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 安装执行创建结果（CR-012 §5.5、US-079）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionCreateResult {

    /** 执行ID */
    private Long executionId;

    /** 尝试序号 */
    private int attemptNo;

    /** 安装许可令牌 */
    private String permitToken;

    /** 许可有效期（仅限制进入 INSTALL_STARTED） */
    private Instant validUntil;

    /** 冻结的任务版本 */
    private Long taskRevision;

    /** 冻结的安装计划版本 */
    private String installPlanVersion;
}
