package net.hwyz.iov.cloud.iov.ota.service.application.service;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
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

import java.util.Comparator;


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
     * <p>IOV-OTA-DSN-CR-017 §5：读取 tb_task 已持久化的 sequenceNo + previousTaskId，不得信任发布请求临时传值；
     * 后续波次缺失前序关系（sequenceNo>0 且 previousTaskId 为空）一律阻断；
     * CANARY／RELEASE 首波仍须执行 US-054 跨阶段门禁。</p>
     *
     * @param nextTaskId 待发布任务ID
     * @return 门禁状态（PASS）
     */
    @Transactional
    public ReleaseGateState checkGateForRelease(Long nextTaskId) {
        Task nextTask = taskRepository.getById(TaskId.of(nextTaskId))
                .orElseThrow(() -> new TaskNotExistException(nextTaskId));

        Long prevTaskId = nextTask.getPreviousTaskId();
        Integer seq = nextTask.getSequenceNo();

        Task prevTask;
        if (prevTaskId != null) {
            prevTask = taskRepository.getById(TaskId.of(prevTaskId))
                    .orElseThrow(() -> new TaskReleaseGateException("前序任务[" + prevTaskId + "]不存在，无法放行"));
            validateReleaseRelation(prevTask, nextTask);
        } else if (seq != null && seq > 0) {
            // CR-017 §5.1：后续波次缺失前序关系 → fail-safe 阻断，不得进入“无前序任务直接 PASS”
            log.warn("任务[{}]为第[{}]波次但缺少前序任务关系（previousTaskId 为空），禁止发布", nextTaskId, seq);
            throw new TaskReleaseGateException(
                    "任务[" + nextTaskId + "]为第[" + seq + "]波次但缺少前序任务关系（previousTaskId 为空），禁止发布");
        } else {
            // phase 首波（sequence_no=0 或历史无序号）：无同 phase 前一波
            if (nextTask.getPhase() == TaskPhase.VALIDATION) {
                log.info("任务[{}]为验证阶段首波，无同阶段前序，按既有首阶段规则放行", nextTaskId);
                return ReleaseGateState.PASS;
            }
            // CR-017 §5.3：CANARY／RELEASE 首波仍必须执行 US-054 跨阶段门禁
            prevTask = findLatestPreviousPhaseTask(nextTask);
            if (prevTask == null) {
                log.warn("任务[{}]为阶段[{}]首波但前序阶段无已完成任务，禁止发布", nextTaskId, nextTask.getPhase());
                throw new TaskReleaseGateException(
                        "任务[" + nextTaskId + "]为阶段[" + nextTask.getPhase().name() + "]首波但前序阶段无已完成任务，禁止发布");
            }
        }

        return evaluateGate(nextTask, prevTask);
    }

    /**
     * 关系完整性校验（IOV-OTA-DSN-CR-017 §5.2）
     * <p>前序与当前同 Activity；不得指向自身；同 phase 时前序序号必须小于当前；
     * 跨 phase 时满足 US-054 阶段顺序。</p>
     */
    private void validateReleaseRelation(Task prevTask, Task nextTask) {
        if (prevTask.getId().getValue().equals(nextTask.getId().getValue())) {
            throw new TaskReleaseGateException("前序任务不能指向自身");
        }
        if (!prevTask.getActivityId().getValue().equals(nextTask.getActivityId().getValue())) {
            throw new TaskReleaseGateException("前序任务[" + prevTask.getId().getValue() + "]不属于同一升级活动，无法作为放行依据");
        }
        if (prevTask.getPhase() == nextTask.getPhase()) {
            int prevSeq = prevTask.getSequenceNo() != null ? prevTask.getSequenceNo() : -1;
            int nextSeq = nextTask.getSequenceNo() != null ? nextTask.getSequenceNo() : Integer.MAX_VALUE;
            if (prevSeq >= nextSeq) {
                throw new TaskReleaseGateException(
                        "前序任务序号[" + prevSeq + "]必须小于当前任务序号[" + nextSeq + "]");
            }
        } else if (prevTask.getPhase().getValue() >= nextTask.getPhase().getValue()) {
            throw new TaskReleaseGateException(
                    "跨阶段前序任务阶段[" + prevTask.getPhase().name() + "]不满足 US-054 阶段顺序");
        }
    }

    /**
     * 查找前序阶段最近（序号最大）的已完成任务（IOV-OTA-DSN-CR-017 §5.3）
     * <p>CANARY 前序阶段为 VALIDATION；RELEASE 前序阶段为 CANARY。</p>
     */
    private Task findLatestPreviousPhaseTask(Task nextTask) {
        TaskPhase prevPhase = switch (nextTask.getPhase()) {
            case CANARY -> TaskPhase.VALIDATION;
            case RELEASE -> TaskPhase.CANARY;
            default -> null;
        };
        if (prevPhase == null) {
            return null;
        }
        return taskRepository.findByActivityId(nextTask.getActivityId()).stream()
                .filter(t -> t.getPhase() == prevPhase)
                .max(Comparator.comparing((Task t) -> t.getSequenceNo() != null ? t.getSequenceNo() : 0)
                        .thenComparing(t -> t.getId().getValue()))
                .orElse(null);
    }

    /**
     * 基于前序正式报告计算/读取门禁（IOV-OTA-DSN-CR-017 与 CR-015 §3.2 共用）
     * <p>已存在已决定的门禁则复用；前序报告缺失/FAIL/PENDING fail-safe；
     * 计算后落库 gate（PASS/FAIL/PENDING）并保留 reportRef 与阈值快照。</p>
     */
    private ReleaseGateState evaluateGate(Task nextTask, Task prevTask) {
        Long nextTaskId = nextTask.getId().getValue();
        Long prevTaskId = prevTask.getId().getValue();

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
