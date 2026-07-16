package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.GateState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskPhaseGate;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskPhaseGateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 阶段门禁领域服务
 * 实现 US-054：跨任务阶段门禁与推进
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhaseGateDomainService {
    
    private final TaskPhaseGateRepository taskPhaseGateRepository;
    
    /**
     * 检查阶段门禁是否通过
     * @param activityId 活动ID
     * @param targetPhase 目标阶段
     * @return 门禁是否通过
     */
    public boolean checkPhaseGate(Long activityId, TaskPhase targetPhase) {
        Optional<TaskPhaseGate> gateOpt = taskPhaseGateRepository.getByActivityIdAndToPhase(activityId, targetPhase);
        
        if (gateOpt.isEmpty()) {
            // 门禁不存在，默认通过
            log.debug("活动[{}]阶段[{}]门禁不存在，默认通过", activityId, targetPhase);
            return true;
        }
        
        TaskPhaseGate gate = gateOpt.get();
        boolean passed = gate.isPassed();
        
        log.info("活动[{}]阶段[{}]门禁检查结果[{}]", activityId, targetPhase, passed ? "通过" : "失败");
        return passed;
    }
    
    /**
     * 创建阶段门禁记录
     * @param activityId 活动ID
     * @param fromPhase 来源阶段
     * @param toPhase 目标阶段
     * @param prevTaskId 前序任务ID
     * @return 创建的门禁记录
     */
    public TaskPhaseGate createPhaseGate(Long activityId, TaskPhase fromPhase, TaskPhase toPhase, Long prevTaskId) {
        TaskPhaseGate gate = TaskPhaseGate.builder()
                .activityId(activityId)
                .fromPhase(fromPhase)
                .toPhase(toPhase)
                .prevTaskId(prevTaskId)
                .gateState(null) // 初始状态为待定
                .override(false)
                .build();
        
        gate = taskPhaseGateRepository.save(gate);
        log.info("创建活动[{}]阶段门禁：[{}] -> [{}]", activityId, fromPhase, toPhase);
        return gate;
    }
    
    /**
     * 更新门禁状态为通过
     * @param gateId 门禁ID
     * @param decidedBy 决策人
     * @param reportRef 报告引用
     */
    public void passGate(Long gateId, String decidedBy, String reportRef) {
        TaskPhaseGate gate = taskPhaseGateRepository.getById(gateId)
                .orElseThrow(() -> new IllegalArgumentException("门禁记录不存在"));
        
        gate.pass(decidedBy, reportRef);
        taskPhaseGateRepository.save(gate);
        
        log.info("门禁[{}]已通过，决策人[{}]", gateId, decidedBy);
    }
    
    /**
     * 更新门禁状态为失败
     * @param gateId 门禁ID
     * @param decidedBy 决策人
     * @param reportRef 报告引用
     */
    public void failGate(Long gateId, String decidedBy, String reportRef) {
        TaskPhaseGate gate = taskPhaseGateRepository.getById(gateId)
                .orElseThrow(() -> new IllegalArgumentException("门禁记录不存在"));
        
        gate.fail(decidedBy, reportRef);
        taskPhaseGateRepository.save(gate);
        
        log.info("门禁[{}]已失败，决策人[{}]", gateId, decidedBy);
    }
    
    /**
     * 人工跳阶授权
     * @param gateId 门禁ID
     * @param decidedBy 决策人
     * @param approvalRef 审批引用
     */
    public void overrideGate(Long gateId, String decidedBy, String approvalRef) {
        TaskPhaseGate gate = taskPhaseGateRepository.getById(gateId)
                .orElseThrow(() -> new IllegalArgumentException("门禁记录不存在"));
        
        gate.override(decidedBy, approvalRef);
        taskPhaseGateRepository.save(gate);
        
        log.info("门禁[{}]已人工跳阶授权，决策人[{}]", gateId, decidedBy);
    }
    
    /**
     * 获取活动的所有阶段门禁
     * @param activityId 活动ID
     * @return 门禁列表
     */
    public List<TaskPhaseGate> listPhaseGates(Long activityId) {
        return taskPhaseGateRepository.listByActivityId(activityId);
    }
    
    /**
     * 获取指定阶段的门禁
     * @param activityId 活动ID
     * @param phase 阶段
     * @return 门禁记录
     */
    public Optional<TaskPhaseGate> getPhaseGate(Long activityId, TaskPhase phase) {
        return taskPhaseGateRepository.getByActivityIdAndToPhase(activityId, phase);
    }
    
    /**
     * 检查是否可以推进到下一阶段
     * @param activityId 活动ID
     * @param currentPhase 当前阶段
     * @return 是否可以推进
     */
    public boolean canAdvanceToNextPhase(Long activityId, TaskPhase currentPhase) {
        TaskPhase nextPhase = getNextPhase(currentPhase);
        if (nextPhase == null) {
            // 已经是最后阶段
            return false;
        }
        
        return checkPhaseGate(activityId, nextPhase);
    }
    
    /**
     * 获取下一个阶段
     * @param currentPhase 当前阶段
     * @return 下一个阶段，如果没有则返回null
     */
    private TaskPhase getNextPhase(TaskPhase currentPhase) {
        return switch (currentPhase) {
            case VALIDATION -> TaskPhase.CANARY;
            case CANARY -> TaskPhase.RELEASE;
            case RELEASE -> null; // 已经是最后阶段
        };
    }
}