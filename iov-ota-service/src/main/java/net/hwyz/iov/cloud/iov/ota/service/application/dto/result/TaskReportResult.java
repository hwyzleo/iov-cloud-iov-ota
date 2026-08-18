package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 任务报告结果（CR-015 §3.2）
 * <p>任务进入终态后生成不可变正式报告（reportVersion 幂等）；执行中查询返回 provisional=true，
 * 不得作为下一任务正式放行依据。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TaskReportResult {

    private Long taskId;

    /** 报告版本（正式报告唯一键） */
    private Integer reportVersion;

    /** 完成率 */
    private BigDecimal completeRate;

    /** 成功率 */
    private BigDecimal successRate;

    /** 失败case分布（JSON） */
    private String failCaseDist;

    /** 生成时间 */
    private Instant genTime;

    /** 是否临时统计（true=执行中，不可作为正式放行依据） */
    private boolean provisional;

    /** 任务当前状态 */
    private String taskState;
}
