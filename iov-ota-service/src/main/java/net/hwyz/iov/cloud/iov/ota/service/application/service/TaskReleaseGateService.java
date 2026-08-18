package net.hwyz.iov.cloud.iov.ota.service.application.service;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskReleaseGateResult;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.TaskNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.TaskReleaseGateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReleaseGate;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.PhaseGatePolicyRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReleaseGateRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TaskReleaseGateDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 多任务放行门禁应用服务（CR-015 §3.2）
 * <p>releaseTask 前校验 sequenceNo/previousTaskId → 锁定前序 Task → 加载前序正式报告 →
 * 计算/读取 release gate → PASS 才继续；FAIL/PENDING fail-safe。人工 override 必须带权限、原因与审批引用。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskReleaseGateService {

    private final TaskRepository taskRepository;
    private final TaskReportRepository taskReportRepository;
    private final TaskReleaseGateRepository taskReleaseGateRepository;
    private final PhaseGatePolicyRepository phaseGatePolicyRepository;
    private final TaskReleaseGateDomainService taskReleaseGateDomainService;

    /**
     * 发布前门禁校验：PASS 放行，FAIL/PENDING 抛异常拦截（fail-safe）
     * @param nextTaskId 待发布任务ID
     * @return 门禁状态（PASS）
     */
    @Transactional
    public ReleaseGateState checkGateForRelease(Long nextTaskId) {
        Task nextTask = taskRepository.getById(TaskId.of(nextTaskId))
                .orElseThrow(() -> new TaskNotExistException(nextTaskId));

        Long prevTaskId = nextTask.getPreviousTaskId();
        if (prevTaskId == null) {
            log.info("任务[{}]无前序任务，无需放行门禁", nextTaskId);
            return ReleaseGateState.PASS;
        }

        Task prevTask = taskRepository.getById(TaskId.of(prevTaskId))
                .orElseThrow(() -> new TaskReleaseGateException("前序任务[" + prevTaskId + "]不存在，无法放行"));
        if (!prevTask.getActivityId().getValue().equals(nextTask.getActivityId().getValue())) {
            throw new TaskReleaseGateException("前序任务[" + prevTaskId + "]不属于同一升级活动，无法作为放行依据");
        }

        // 已存在已决定的门禁（PASS/FAIL，含人工 override）则直接复用，不重复计算
        TaskReleaseGate existing = taskReleaseGateRepository.getByNextTaskId(nextTaskId).orElse(null);
        if (existing != null && existing.getGateState() != ReleaseGateState.PENDING) {
            log.info("任务[{}]已有门禁结论[{}]，复用", nextTaskId, existing.getGateState());
            if (existing.isPassed()) {
                return ReleaseGateState.PASS;
            }
            throw new TaskReleaseGateException("前序任务[" + prevTaskId + "]放行门禁为[" + existing.getGateState() + "]，拦截发布任务[" + nextTaskId + "]");
        }

        // 加载前序正式报告（缺失 → PENDING fail-safe）
        TaskReport report = taskReportRepository.findLatestByTaskId(prevTaskId).orElse(null);
        if (report == null) {
            log.warn("前序任务[{}]无正式报告，门禁置为PENDING并拦截发布任务[{}]", prevTaskId, nextTaskId);
            recordGate(existing, nextTask, prevTaskId, ReleaseGateState.PENDING, null, null);
            throw new TaskReleaseGateException("前序任务[" + prevTaskId + "]尚无正式报告，禁止发布任务[" + nextTaskId + "]");
        }

        // 门禁阈值策略（活动级覆盖优先）
        PhaseGatePolicy policy = phaseGatePolicyRepository
                .findByPhaseAndActivity(prevTask.getPhase().getValue(), nextTask.getActivityId().getValue())
                .orElse(null);

        ReleaseGateState state = taskReleaseGateDomainService.evaluate(policy, report.getSuccessRate(), failCntFromReport(report));
        String reportRef = report.getTaskId() + ":" + report.getReportVersion();
        String snapshot = taskReleaseGateDomainService.toThresholdSnapshot(policy);
        recordGate(existing, nextTask, prevTaskId, state, reportRef, snapshot);

        log.info("任务[{}]放行门禁结论[{}]，前序任务[{}]报告[{}]", nextTaskId, state, prevTaskId, reportRef);
        if (state != ReleaseGateState.PASS) {
            throw new TaskReleaseGateException("前序任务[" + prevTaskId + "]放行门禁为[" + state + "]，拦截发布任务[" + nextTaskId + "]");
        }
        return ReleaseGateState.PASS;
    }

    /**
     * 查询某任务对其下一任务的放行结论（previous_task_id = 该任务）
     */
    @Transactional(readOnly = true)
    public TaskReleaseGateResult queryGateForTask(Long taskId) {
        Task task = taskRepository.getById(TaskId.of(taskId))
                .orElseThrow(() -> new TaskNotExistException(taskId));

        TaskReleaseGate gate = taskReleaseGateRepository.getByPreviousTaskId(taskId).orElse(null);
        if (gate == null) {
            return TaskReleaseGateResult.builder()
                    .activityId(task.getActivityId().getValue())
                    .previousTaskId(taskId)
                    .gateState(ReleaseGateState.PENDING.getValue())
                    .build();
        }
        return toResult(gate);
    }

    /**
     * 人工放行（override）：必须携带决策人、审批引用与原因
     */
    @Transactional
    public TaskReleaseGateResult overrideGateForNextTask(Long nextTaskId, String decidedBy, String approvalRef, String reason) {
        TaskReleaseGate gate = taskReleaseGateRepository.getByNextTaskId(nextTaskId)
                .orElseThrow(() -> new TaskReleaseGateException("任务[" + nextTaskId + "]尚无放行门禁记录，无法人工放行"));
        gate.override(decidedBy, approvalRef, reason);
        taskReleaseGateRepository.save(gate);
        log.info("任务[{}]放行门禁已人工放行，决策人[{}]，审批引用[{}]", nextTaskId, decidedBy, approvalRef);
        return toResult(gate);
    }

    private void recordGate(TaskReleaseGate existing, Task nextTask, Long prevTaskId,
                            ReleaseGateState state, String reportRef, String snapshot) {
        if (existing == null) {
            TaskReleaseGate.ReleaseGateType gateType = resolveGateType(prevTaskId, nextTask);
            existing = TaskReleaseGate.builder()
                    .activityId(nextTask.getActivityId().getValue())
                    .previousTaskId(prevTaskId)
                    .nextTaskId(nextTask.getId().getValue())
                    .gateType(gateType)
                    .gateState(state)
                    .gateThresholdSnapshot(snapshot)
                    .reportRef(reportRef)
                    .override(false)
                    .build();
        } else {
            existing.setGateState(state);
            existing.setGateThresholdSnapshot(snapshot);
            existing.setReportRef(reportRef);
        }
        taskReleaseGateRepository.save(existing);
    }

    /**
     * 门禁类型：前序与下一任务同阶段为 SAME_PHASE，跨阶段为 CROSS_PHASE
     */
    private TaskReleaseGate.ReleaseGateType resolveGateType(Long prevTaskId, Task nextTask) {
        Task prevTask = taskRepository.getById(TaskId.of(prevTaskId)).orElse(null);
        boolean samePhase = prevTask != null && prevTask.getPhase() == nextTask.getPhase();
        return samePhase
                ? TaskReleaseGate.ReleaseGateType.SAME_PHASE
                : TaskReleaseGate.ReleaseGateType.CROSS_PHASE;
    }

    /**
     * 从正式报告 failCaseDist（JSON：SUCCEEDED/FAILED/ROLLED_BACK/TIMED_OUT）解析失败数
     */
    private Integer failCntFromReport(TaskReport report) {
        if (report.getFailCaseDist() == null || report.getFailCaseDist().isBlank()) {
            return 0;
        }
        try {
            JSONObject dist = new JSONObject(report.getFailCaseDist());
            int failed = dist.getInt("FAILED", 0);
            int rolledBack = dist.getInt("ROLLED_BACK", 0);
            return failed + rolledBack;
        } catch (Exception e) {
            log.warn("解析正式报告失败分布失败，按0处理: {}", report.getFailCaseDist());
            return 0;
        }
    }

    private TaskReleaseGateResult toResult(TaskReleaseGate gate) {
        return TaskReleaseGateResult.builder()
                .activityId(gate.getActivityId())
                .previousTaskId(gate.getPreviousTaskId())
                .nextTaskId(gate.getNextTaskId())
                .gateType(gate.getGateType() != null ? gate.getGateType().name() : null)
                .gateState(gate.getGateState() != null ? gate.getGateState().getValue() : ReleaseGateState.PENDING.getValue())
                .gateThresholdSnapshot(gate.getGateThresholdSnapshot())
                .reportRef(gate.getReportRef())
                .override(gate.getOverride())
                .approvalRef(gate.getApprovalRef())
                .decidedBy(gate.getDecidedBy())
                .decidedAt(gate.getDecidedAt())
                .description(gate.getDescription())
                .build();
    }
}
