package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReleaseGate;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReleaseGateRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 放行门禁领域服务（CR-015，泛化自旧阶段门禁 PhaseGateDomainService）
 * <p>实现 US-054/CR-015：跨任务放行门禁与阶段推进。
 * 门禁挂在下一任务（nextTaskId）上；CROSS_PHASE 表达跨阶段推进，SAME_PHASE 表达同阶段波次。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhaseGateDomainService {

    private final TaskReleaseGateRepository taskReleaseGateRepository;
    private final TaskRepository taskRepository;

    /**
     * 检查阶段门禁是否通过
     * @param activityId 活动ID
     * @param targetPhase 目标阶段
     * @return 门禁是否通过（门禁不存在默认通过）
     */
    public boolean checkPhaseGate(Long activityId, TaskPhase targetPhase) {
        Optional<TaskReleaseGate> gateOpt = getGateForPhase(activityId, targetPhase);

        if (gateOpt.isEmpty()) {
            log.debug("活动[{}]阶段[{}]门禁不存在，默认通过", activityId, targetPhase);
            return true;
        }

        TaskReleaseGate gate = gateOpt.get();
        boolean passed = gate.isPassed();

        log.info("活动[{}]阶段[{}]门禁检查结果[{}]", activityId, targetPhase, passed ? "通过" : "拦截");
        return passed;
    }

    /**
     * 创建跨阶段放行门禁记录
     * @param activityId 活动ID
     * @param prevTaskId 前序任务ID
     * @param nextTaskId 下一任务ID
     * @return 创建的门禁记录
     */
    public TaskReleaseGate createPhaseGate(Long activityId, Long prevTaskId, Long nextTaskId) {
        TaskReleaseGate gate = TaskReleaseGate.builder()
                .activityId(activityId)
                .previousTaskId(prevTaskId)
                .nextTaskId(nextTaskId)
                .gateType(TaskReleaseGate.ReleaseGateType.CROSS_PHASE)
                .gateState(ReleaseGateState.PENDING)
                .override(false)
                .build();

        gate = taskReleaseGateRepository.save(gate);
        log.info("创建活动[{}]跨阶段放行门禁：prev[{}] -> next[{}]", activityId, prevTaskId, nextTaskId);
        return gate;
    }

    /**
     * 更新门禁状态为通过
     */
    public void passGate(Long gateId, String decidedBy, String reportRef) {
        TaskReleaseGate gate = taskReleaseGateRepository.getById(gateId)
                .orElseThrow(() -> new IllegalArgumentException("放行门禁记录不存在"));
        gate.pass(decidedBy, reportRef);
        taskReleaseGateRepository.save(gate);
        log.info("门禁[{}]已通过，决策人[{}]", gateId, decidedBy);
    }

    /**
     * 更新门禁状态为拦截
     */
    public void failGate(Long gateId, String decidedBy, String reportRef) {
        TaskReleaseGate gate = taskReleaseGateRepository.getById(gateId)
                .orElseThrow(() -> new IllegalArgumentException("放行门禁记录不存在"));
        gate.fail(decidedBy, reportRef);
        taskReleaseGateRepository.save(gate);
        log.info("门禁[{}]已拦截，决策人[{}]", gateId, decidedBy);
    }

    /**
     * 人工放行（override）
     */
    public void overrideGate(Long gateId, String decidedBy, String approvalRef, String reason) {
        TaskReleaseGate gate = taskReleaseGateRepository.getById(gateId)
                .orElseThrow(() -> new IllegalArgumentException("放行门禁记录不存在"));
        gate.override(decidedBy, approvalRef, reason);
        taskReleaseGateRepository.save(gate);
        log.info("门禁[{}]已人工放行，决策人[{}]", gateId, decidedBy);
    }

    /**
     * 获取活动的所有放行门禁
     */
    public List<TaskReleaseGate> listPhaseGates(Long activityId) {
        return taskReleaseGateRepository.listByActivityId(activityId);
    }

    /**
     * 获取指定阶段的门禁（通过该阶段下存在任务解析）
     */
    public Optional<TaskReleaseGate> getPhaseGate(Long activityId, TaskPhase phase) {
        return getGateForPhase(activityId, phase);
    }

    /**
     * 检查是否可以推进到下一阶段
     */
    public boolean canAdvanceToNextPhase(Long activityId, TaskPhase currentPhase) {
        TaskPhase nextPhase = getNextPhase(currentPhase);
        if (nextPhase == null) {
            return false;
        }
        return checkPhaseGate(activityId, nextPhase);
    }

    /**
     * 解析活动内指定阶段的任务，并返回其放行门禁
     */
    private Optional<TaskReleaseGate> getGateForPhase(Long activityId, TaskPhase phase) {
        Optional<Task> phaseTask = taskRepository.findByActivityId(ActivityId.of(activityId)).stream()
                .filter(t -> t.getPhase() == phase)
                .findFirst();
        return phaseTask.flatMap(t -> taskReleaseGateRepository.getByNextTaskId(t.getId().getValue()));
    }

    private TaskPhase getNextPhase(TaskPhase currentPhase) {
        return switch (currentPhase) {
            case VALIDATION -> TaskPhase.CANARY;
            case CANARY -> TaskPhase.RELEASE;
            case RELEASE -> null; // 已经是最后阶段
        };
    }
}
