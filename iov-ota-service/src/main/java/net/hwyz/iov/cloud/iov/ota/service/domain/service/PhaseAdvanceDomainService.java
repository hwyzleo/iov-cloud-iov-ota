package net.hwyz.iov.cloud.iov.ota.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskPhaseGate;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 阶段推进领域服务
 * 处理跨任务阶段门禁与推进逻辑（US-054）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhaseAdvanceDomainService {
    
    private final PhaseGateDomainService phaseGateDomainService;
    private final TaskRepository taskRepository;
    
    /**
     * 尝试推进到下一阶段
     * 当任务完成时调用，检查门禁并决定是否创建下一阶段任务
     * @param completedTask 已完成的任务
     * @return 是否成功推进
     */
    public boolean tryAdvanceToNextPhase(Task completedTask) {
        ActivityId activityId = completedTask.getActivityId();
        TaskPhase currentPhase = completedTask.getPhase();
        
        log.info("尝试推进活动[{}]从阶段[{}]到下一阶段", activityId.getValue(), currentPhase);
        
        // 检查是否可以推进到下一阶段
        if (!phaseGateDomainService.canAdvanceToNextPhase(activityId.getValue(), currentPhase)) {
            log.info("活动[{}]阶段[{}]门禁未通过，无法推进", activityId.getValue(), currentPhase);
            return false;
        }
        
        // 获取下一阶段
        TaskPhase nextPhase = getNextPhase(currentPhase);
        if (nextPhase == null) {
            log.info("活动[{}]已是最后阶段，无法推进", activityId.getValue());
            return false;
        }
        
        // 检查是否已存在下一阶段的任务
        Optional<Task> existingTask = findExistingTaskForPhase(activityId, nextPhase);
        if (existingTask.isPresent()) {
            log.info("活动[{}]阶段[{}]已存在任务[{}]，跳过创建", 
                    activityId.getValue(), nextPhase, existingTask.get().getId().getValue());
            return true;
        }
        
        // 创建下一阶段任务
        Task newTask = createNextPhaseTask(completedTask, nextPhase);
        taskRepository.save(newTask);
        
        log.info("为活动[{}]创建阶段[{}]任务[{}]", 
                activityId.getValue(), nextPhase, newTask.getId().getValue());
        
        return true;
    }
    
    /**
     * 获取下一个阶段
     */
    private TaskPhase getNextPhase(TaskPhase currentPhase) {
        return switch (currentPhase) {
            case VALIDATION -> TaskPhase.CANARY;
            case CANARY -> TaskPhase.RELEASE;
            case RELEASE -> null; // 已经是最后阶段
        };
    }
    
    /**
     * 查找指定阶段已存在的任务
     */
    private Optional<Task> findExistingTaskForPhase(ActivityId activityId, TaskPhase phase) {
        // 这里需要根据activityId和phase查询任务
        // 简化实现：查询该活动下所有任务，找到指定阶段的任务
        // 实际应该通过仓储方法查询
        return Optional.empty(); // TODO: 实现查询逻辑
    }
    
    /**
     * 创建下一阶段任务
     * 基于已完成任务的信息创建新任务
     */
    private Task createNextPhaseTask(Task completedTask, TaskPhase nextPhase) {
        // 生成新的任务ID（实际应该由仓储生成）
        TaskId newTaskId = TaskId.of(System.currentTimeMillis());
        
        // 创建新任务，复制基本信息
        Task newTask = Task.create(
                newTaskId,
                completedTask.getName() + " - " + nextPhase.name(),
                completedTask.getType(),
                completedTask.getActivityId()
        );
        
        // 设置阶段（固定不可变）
        newTask.setPhase(nextPhase);
        
        // 复制其他配置
        newTask.setTarget(completedTask.getTarget());
        newTask.setStartTime(completedTask.getStartTime());
        newTask.setEndTime(completedTask.getEndTime());
        newTask.setUpgradeMode(completedTask.getUpgradeMode());
        newTask.setUpgradeModeArg(completedTask.getUpgradeModeArg());
        
        return newTask;
    }
    
    /**
     * 手动触发阶段推进（人工跳阶授权）
     * @param activityId 活动ID
     * @param fromPhase 来源阶段
     * @param toPhase 目标阶段
     * @param decidedBy 决策人
     * @param approvalRef 审批引用
     * @return 是否成功推进
     */
    public boolean manualAdvancePhase(Long activityId, TaskPhase fromPhase, TaskPhase toPhase, 
                                     String decidedBy, String approvalRef) {
        log.info("人工跳阶授权：活动[{}]从[{}]到[{}]", activityId, fromPhase, toPhase);
        
        // 获取门禁记录
        Optional<TaskPhaseGate> gateOpt = phaseGateDomainService.getPhaseGate(activityId, toPhase);
        if (gateOpt.isEmpty()) {
            log.warn("活动[{}]阶段[{}]门禁不存在", activityId, toPhase);
            return false;
        }
        
        // 执行跳阶授权
        phaseGateDomainService.overrideGate(gateOpt.get().getId(), decidedBy, approvalRef);
        
        // 查找来源阶段的任务
        Optional<Task> fromTask = findExistingTaskForPhase(ActivityId.of(activityId), fromPhase);
        if (fromTask.isEmpty()) {
            log.warn("活动[{}]阶段[{}]任务不存在", activityId, fromPhase);
            return false;
        }
        
        // 创建目标阶段任务
        Task newTask = createNextPhaseTask(fromTask.get(), toPhase);
        taskRepository.save(newTask);
        
        log.info("人工跳阶授权完成，创建任务[{}]", newTask.getId().getValue());
        return true;
    }
}