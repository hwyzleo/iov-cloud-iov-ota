package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskMetricResult;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.TaskNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskMetric;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.PhaseGatePolicyRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskMetricRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * 任务健康指标查询服务（CR-015 §3.2）
 * <p>指标以 tb_task_vehicle 与 tb_task_vehicle_execution 权威状态聚合；
 * GatewayDeliveryStatus 不计入业务成功率。聚合结果快照写入 tb_task_metric。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskMetricQueryService {

    private final TaskRepository taskRepository;
    private final TaskVehicleMapper taskVehicleMapper;
    private final TaskMetricRepository taskMetricRepository;
    private final PhaseGatePolicyRepository phaseGatePolicyRepository;

    /**
     * 聚合单任务健康指标并快照
     */
    @Transactional
    public TaskMetricResult getMetric(Long taskId) {
        Task task = taskRepository.getById(TaskId.of(taskId))
                .orElseThrow(() -> new TaskNotExistException(taskId));

        int successCnt = taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(taskId, "SUCCEEDED");
        int failCnt = taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(taskId, "FAILED")
                + taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(taskId, "ROLLED_BACK");
        int timeoutCnt = taskVehicleMapper.countTimeoutExecutionByTaskId(taskId);
        int totalCnt = taskVehicleMapper.countAllByTaskId(taskId);

        int decidedCnt = successCnt + failCnt + timeoutCnt;
        BigDecimal completeRate = totalCnt > 0
                ? BigDecimal.valueOf(successCnt + failCnt).divide(BigDecimal.valueOf(totalCnt), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal successRate = decidedCnt > 0
                ? BigDecimal.valueOf(successCnt).divide(BigDecimal.valueOf(decidedCnt), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal failRate = decidedCnt > 0
                ? BigDecimal.valueOf(failCnt + timeoutCnt).divide(BigDecimal.valueOf(decidedCnt), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 门禁阈值策略（活动级覆盖优先）
        PhaseGatePolicy policy = phaseGatePolicyRepository
                .findByPhaseAndActivity(task.getPhase().getValue(), task.getActivityId().getValue())
                .orElse(null);
        BigDecimal gateThreshold = policy != null ? policy.getSuccessRateMin() : null;
        boolean breach = policy != null
                && policy.getSuccessRateMin() != null
                && successRate.compareTo(policy.getSuccessRateMin()) < 0;
        String gateState = breach ? "BREACH" : "OK";

        // 快照写入 tb_task_metric
        taskMetricRepository.save(new TaskMetric()
                .setTaskId(taskId)
                .setSuccessCnt(successCnt)
                .setFailCnt(failCnt)
                .setTimeoutCnt(timeoutCnt)
                .setFailRate(failRate)
                .setGateThreshold(gateThreshold)
                .setGateState(gateState)
                .setStatTime(Instant.now()));

        return TaskMetricResult.builder()
                .taskId(taskId)
                .successCnt(successCnt)
                .failCnt(failCnt)
                .timeoutCnt(timeoutCnt)
                .totalCnt(totalCnt)
                .completeRate(completeRate)
                .successRate(successRate)
                .failRate(failRate)
                .gateThreshold(gateThreshold)
                .gateState(gateState)
                .statTime(Instant.now())
                .build();
    }
}
