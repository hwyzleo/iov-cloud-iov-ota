package net.hwyz.iov.cloud.iov.ota.service.application.dto.result;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 任务健康指标结果（CR-015 §3.2）
 * <p>以 tb_task_vehicle 与 tb_task_vehicle_execution 权威状态聚合；
 * GatewayDeliveryStatus 仅作为独立技术维度，不计入业务成功率。</p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class TaskMetricResult {

    private Long taskId;

    /** 成功车辆数（vehicle_task_status=SUCCEEDED） */
    private Integer successCnt;

    /** 失败车辆数（vehicle_task_status in FAILED/ROLLED_BACK） */
    private Integer failCnt;

    /** 超时执行数（execution status=TIMED_OUT） */
    private Integer timeoutCnt;

    /** 任务车辆总数 */
    private Integer totalCnt;

    /** 完成率（(成功+失败)/总数） */
    private BigDecimal completeRate;

    /** 成功率（成功/(成功+失败)） */
    private BigDecimal successRate;

    /** 失败率（(失败+超时)/(成功+失败+超时)） */
    private BigDecimal failRate;

    /** 门禁阈值（success_rate_min，取自门禁策略） */
    private BigDecimal gateThreshold;

    /** 门禁状态：OK/BREACH（依据阈值判定） */
    private String gateState;

    /** 统计时间 */
    private Instant statTime;
}
