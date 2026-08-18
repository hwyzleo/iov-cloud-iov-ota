package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.query.TaskQuery;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskResult;
import net.hwyz.iov.cloud.iov.ota.service.application.assembler.TaskAssembler;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskOrder;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskInstallConditionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReleaseGateRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.event.publisher.DomainEventPublisher;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.TaskNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.TaskOrderException;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OptimisticLockException;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TargetResolutionDomainService;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ApprovalDomainService;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ApprovalLevel;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskApproval;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReleaseGate;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAppService {

    private final TaskRepository taskRepository;
    private final TaskInstallConditionRepository taskInstallConditionRepository;
    private final TaskAssembler taskAssembler;
    private final DomainEventPublisher eventPublisher;
    private final ActivityAppService activityAppService;
    private final ApprovalDomainService approvalDomainService;
    private final TargetResolutionDomainService targetResolutionDomainService;
    private final TaskReleaseGateService taskReleaseGateService;
    private final TaskReportAppService taskReportAppService;
    private final TaskReportRepository taskReportRepository;
    private final TaskReleaseGateRepository taskReleaseGateRepository;

    public List<TaskResult> search(String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return taskRepository.findReleasedTasks().stream()
            .filter(task -> {
                if (name != null && !task.getName().contains(name)) return false;
                if (beginTime != null && task.getStartTime().isBefore(beginTime.toInstant())) return false;
                if (endTime != null && task.getEndTime().isAfter(endTime.toInstant())) return false;
                return true;
            })
            .map(task -> enrichTaskResult(taskAssembler.toResult(task)))
            .collect(Collectors.toList());
    }

    public TaskResult getTaskById(Long id) {
        Task task = taskRepository.getById(TaskId.of(id))
            .orElseThrow(() -> new TaskNotExistException(id));
        return enrichTaskResult(taskAssembler.toResult(task));
    }

    @Transactional
    public TaskResult createTask(TaskCreateCmd cmd) {
        log.info("创建任务: {}", cmd.getName());

        validateTaskWindowWithinActivity(cmd.getActivityId(), cmd.getStartTime(), cmd.getEndTime());

        // US-061: 废弃 TaskType.LIGHT，type 默认为 NORMAL（兼容数字字符串与枚举名，如 @Builder.Default 的 "NORMAL"）
        TaskType taskType = (cmd.getType() != null && !cmd.getType().isEmpty())
            ? parseTaskType(cmd.getType()) : TaskType.NORMAL;

        // IOV-OTA-DSN-CR-017：锁定 Activity 聚合根，以 Activity 粒度串行化 Task 创建，保证并发排号唯一
        taskRepository.lockActivity(cmd.getActivityId());

        // UI 创建路径不传 phase，领域工厂固定为 VALIDATION（CR-009 phase 不可变）
        TaskPhase phase = TaskPhase.VALIDATION;
        Long newTaskId = generateId();

        // 解析波次序（CR-017 §3.2）：expectedSequence = COALESCE(MAX(sequence_no), -1) + 1
        long expectedSequence = resolveExpectedSequence(cmd.getActivityId(), phase);
        long resolvedSequence;
        if (cmd.getSequenceNo() == null) {
            resolvedSequence = expectedSequence;
        } else if (cmd.getSequenceNo() == expectedSequence) {
            resolvedSequence = cmd.getSequenceNo();
        } else {
            // 客户端不得通过显式值插队、复用旧序号或制造跳号
            throw TaskOrderException.sequenceConflict(cmd.getActivityId(), (int) expectedSequence, cmd.getSequenceNo());
        }

        // 解析前序任务（CR-017 §3.3）：缺省自动绑定同作用域 sequence-1，显式值必须结构合法
        Long resolvedPreviousTaskId = resolvePreviousTaskId(cmd, phase, resolvedSequence, newTaskId);

        Task task = Task.create(
            TaskId.of(newTaskId),
            cmd.getName(),
            taskType,
            ActivityId.of(cmd.getActivityId()),
            TaskOrder.laterWave(resolvedSequence, resolvedPreviousTaskId)
        );
        task.setTarget(cmd.getTarget());
        task.setStartTime(cmd.getStartTime());
        task.setEndTime(cmd.getEndTime());
        task.setNoticeType(cmd.getNoticeType());
        task.setUpgradeMode(cmd.getUpgradeMode() != null && !cmd.getUpgradeMode().isEmpty() 
            ? net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeMode.valOf(Integer.parseInt(cmd.getUpgradeMode())) : null);
        task.setDescription(cmd.getDescription());

        if (cmd.getRestrictions() != null) {
            task.loadRestrictionsAndStrategies(
                taskAssembler.toRestrictions(cmd.getRestrictions()),
                taskAssembler.toStrategies(cmd.getStrategies())
            );
        }

        // 自动排号、前序查询、Task 写入与创建审计在同一数据库事务内完成
        taskRepository.save(task);

        // 保存安装条件
        if (cmd.getInstallConditions() != null && !cmd.getInstallConditions().isEmpty()) {
            List<net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition> installConditions =
                taskAssembler.toInstallConditions(cmd.getInstallConditions());
            for (net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition condition : installConditions) {
                condition.setId(null);
                condition.setTaskId(task.getId().getValue());
                taskInstallConditionRepository.save(condition);
            }
        }

        log.info("任务[{}]创建完成：activityId[{}] phase[{}] sequenceNo[{}] previousTaskId[{}] relationSource[{}]",
                task.getId().getValue(), cmd.getActivityId(), phase.name(), resolvedSequence, resolvedPreviousTaskId,
                cmd.getPreviousTaskId() != null ? "EXPLICIT" : "AUTO");

        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return enrichTaskResult(taskAssembler.toResult(task));
    }

    @Transactional
    public TaskResult submitTask(TaskSubmitCmd cmd) {
        log.info("提交任务: {}", cmd.getTaskId());

        Task task = taskRepository.getById(TaskId.of(cmd.getTaskId()))
            .orElseThrow(() -> new TaskNotExistException(cmd.getTaskId()));

        taskAssembler.updateFromCmd(task, cmd);
        task.submit();

        // 检查阶段是否需要审批，如果不需要则直接通过
        if (approvalDomainService.checkApprovalRequirements(
            cmd.getTaskId(), task.getPhase(), task.getActivityId().getValue())) {
            task.approve(true, null);
            log.info("任务[{}]阶段[{}]不需要审批，直接通过", cmd.getTaskId(), task.getPhase());
        }

        taskRepository.save(task);

        // 更新安装条件
        if (cmd.getInstallConditions() != null && !cmd.getInstallConditions().isEmpty()) {
            // 删除原有安装条件
            taskInstallConditionRepository.deleteByTaskId(cmd.getTaskId());
            // 保存新的安装条件
            List<net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition> installConditions =
                taskAssembler.toInstallConditions(cmd.getInstallConditions());
            for (net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskInstallCondition condition : installConditions) {
                condition.setId(null);
                condition.setTaskId(cmd.getTaskId());
                taskInstallConditionRepository.save(condition);
            }
        }

        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();

        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult auditTask(TaskAuditCmd cmd) {
        log.info("审批任务: {}, 级别: {}, 结果: {}", cmd.getTaskId(), cmd.getApprovalLevel(), cmd.getResult());
        
        Task task = taskRepository.getById(TaskId.of(cmd.getTaskId()))
            .orElseThrow(() -> new TaskNotExistException(cmd.getTaskId()));
        
        // 提交审批记录
        ApprovalLevel level = ApprovalLevel.valueOf(cmd.getApprovalLevel());
        TaskApproval approval = approvalDomainService.submitApproval(
            cmd.getTaskId(), 
            level, 
            SecurityUtils.getUserId().toString(), 
            cmd.getResult(), 
            cmd.getComment()
        );
        
        // 根据审批结果更新任务状态
        if ("REJECTED".equals(cmd.getResult())) {
            // 任一级别拒绝，任务状态变为REJECTED
            task.approve(false, cmd.getComment());
        } else if ("APPROVED".equals(cmd.getResult())) {
            // 检查是否所有级别都已审批通过
            boolean allApproved = approvalDomainService.checkApprovalRequirements(
                cmd.getTaskId(), 
                task.getPhase(), 
                task.getActivityId().getValue()
            );
            
            if (allApproved) {
                // 所有级别都已通过，任务状态变为APPROVED
                task.approve(true, null);
            } else {
                // 还有其他级别需要审批，任务状态保持PENDING_APPROVAL
                log.info("任务[{}]在[{}]级别审批通过，等待后续审批", cmd.getTaskId(), cmd.getApprovalLevel());
            }
        }
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    /**
     * 统一发布任务（立即发布）
     * 状态CAS、车辆快照、条件快照、状态日志和Outbox在同一数据库事务内提交
     */
    @Transactional
    public TaskResult releaseTask(Long taskId) {
        log.info("立即发布任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        // US-060: 审批与阶段结合 - 发布前检查审批要求
        if (!approvalDomainService.checkApprovalRequirements(taskId, task.getPhase(), task.getActivityId().getValue())) {
            throw new IllegalStateException("任务[" + taskId + "]未满足阶段[" + task.getPhase() + "]的审批要求");
        }

        // CR-015: 多任务放行门禁 - 校验前序正式报告并计算/读取门禁，PASS 才继续；FAIL/PENDING fail-safe
        taskReleaseGateService.checkGateForRelease(taskId);
        
        // 解析目标定义，获取车辆集合
        Set<Vin> vehicles = targetResolutionDomainService.resolveTarget(task.getTarget());
        
        // 统一发布事务：IMMEDIATE触发方式
        task.release(vehicles, "IMMEDIATE");
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }
    
    /**
     * 统一发布任务（到点发布，由调度器触发）
     */
    @Transactional
    public TaskResult releaseTaskByScheduler(Long taskId) {
        log.info("到点发布任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));

        // CR-015: 多任务放行门禁 - 校验前序正式报告并计算/读取门禁，PASS 才继续；FAIL/PENDING fail-safe
        taskReleaseGateService.checkGateForRelease(taskId);

        // 解析目标定义，获取车辆集合
        Set<Vin> vehicles = targetResolutionDomainService.resolveTarget(task.getTarget());
        
        // 统一发布事务：SCHEDULER触发方式
        task.release(vehicles, "SCHEDULER");
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult pauseTask(Long taskId) {
        log.info("暂停任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.pause();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult resumeTask(Long taskId) {
        log.info("恢复任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.resume();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult cancelTask(Long taskId) {
        log.info("取消任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.cancel();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();

        // CR-015: 终态生成不可变正式报告
        taskReportAppService.generateFormalReport(taskId);
        
        return taskAssembler.toResult(task);
    }

    /**
     * 排程任务（定时发布，CR-015 §5 乐观锁）
     * @param taskId       任务ID
     * @param releaseTime  计划发布时间
     * @param rowVersion   乐观锁版本（前端携带；为空时取库内当前值）
     */
    @Transactional
    public TaskResult scheduleTask(Long taskId, Instant releaseTime, Integer rowVersion) {
        log.info("排程任务: {}, 计划发布时间: {}, rowVersion: {}", taskId, releaseTime, rowVersion);

        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));

        // 乐观锁基准：优先使用前端回传的 rowVersion，否则取库内当前版本
        Integer expectedRowVersion = rowVersion != null
                ? rowVersion
                : taskRepository.getRowVersion(TaskId.of(taskId));

        task.schedule(releaseTime);

        boolean updated = taskRepository.scheduleWithOptimisticLock(task, expectedRowVersion);
        if (!updated) {
            throw new OptimisticLockException("任务[" + taskId + "]已被他人修改，请刷新后重试");
        }

        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }
    
    /**
     * 取消排程
     * @param taskId 任务ID
     */
    @Transactional
    public TaskResult unscheduleTask(Long taskId) {
        log.info("取消排程任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.unschedule();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }
    
    /**
     * 激活放量（首批放量或首车领取）
     * @param taskId 任务ID
     */
    @Transactional
    public TaskResult activateRollout(Long taskId) {
        log.info("激活放量任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.activateRollout();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult pauseTaskWithReason(Long taskId, String pauseReason, String pausedBy) {
        log.info("暂停任务: {}, 原因: {}, 发起方: {}", taskId, pauseReason, pausedBy);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.pause(pauseReason, pausedBy);
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult cancelTaskWithReason(Long taskId, String cancelReason) {
        log.info("取消任务: {}, 原因: {}", taskId, cancelReason);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.cancel(cancelReason);
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();

        // CR-015: 终态生成不可变正式报告
        taskReportAppService.generateFormalReport(taskId);
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult supersedeTask(Long taskId) {
        log.info("取代任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.supersede();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();

        // CR-015: 终态生成不可变正式报告
        taskReportAppService.generateFormalReport(taskId);
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult finishTask(Long taskId) {
        log.info("结束任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.finish();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();

        // CR-015: 终态生成不可变正式报告
        taskReportAppService.generateFormalReport(taskId);
        
        return taskAssembler.toResult(task);
    }

    public int deleteTaskByIds(Long[] ids) {
        // IOV-OTA-DSN-CR-017：被后续任务引用为前序的 Task 只能取消，不得删除并重排
        for (Long id : ids) {
            if (taskRepository.isReferencedAsPrevious(id)) {
                throw new IllegalStateException("任务[" + id + "]已被后续任务引用为前序，只能取消，不能删除");
            }
        }
        List<TaskId> taskIdList = List.of(ids).stream()
            .map(TaskId::of)
            .collect(Collectors.toList());
        taskRepository.deleteAll(taskIdList);
        return ids.length;
    }

    private Long generateId() {
        return System.currentTimeMillis();
    }

    /**
     * 解析任务类型：兼容数字字符串（"1"/"2"）与枚举名（"NORMAL"/"LIGHT"），非法值回落 NORMAL
     */
    private TaskType parseTaskType(String type) {
        try {
            return TaskType.valOf(Integer.parseInt(type));
        } catch (NumberFormatException e) {
            try {
                return TaskType.valueOf(type);
            } catch (IllegalArgumentException ex) {
                log.warn("无法识别的任务类型[{}]，回落 NORMAL", type);
                return TaskType.NORMAL;
            }
        }
    }

    /**
     * 计算 (activityId, phase) 作用域内下一个可分配波次序（IOV-OTA-DSN-CR-017 §3.2）
     * <p>expectedSequence = COALESCE(MAX(sequence_no), -1) + 1；首个 Task 得到 0。</p>
     */
    private long resolveExpectedSequence(Long activityId, TaskPhase phase) {
        Long maxSeq = taskRepository.findMaxSequence(activityId, phase);
        return (maxSeq == null ? -1L : maxSeq) + 1L;
    }

    /**
     * 解析前序任务ID（IOV-OTA-DSN-CR-017 §3.3）
     * <p>未显式提供时：sequence=0 落 NULL（phase 首波）；sequence&gt;0 自动绑定同作用域 sequence-1 的唯一 Task；
     * 候选缺失或歧义 fail-closed。显式提供时校验结构合法性并覆盖自动候选。</p>
     */
    private Long resolvePreviousTaskId(TaskCreateCmd cmd, TaskPhase phase, long resolvedSequence, Long newTaskId) {
        if (cmd.getPreviousTaskId() != null) {
            Task prev = taskRepository.getById(TaskId.of(cmd.getPreviousTaskId()))
                    .orElseThrow(() -> TaskOrderException.previousNotFound(cmd.getPreviousTaskId()));
            validateExplicitPrevious(prev, cmd.getActivityId(), phase, resolvedSequence, newTaskId);
            return prev.getId().getValue();
        }
        if (resolvedSequence == 0) {
            return null;
        }
        // 自动推导：同一 (activityId, phase) 且 sequenceNo = resolvedSequence - 1 的唯一 Task
        List<Task> candidates = taskRepository.findByActivityPhaseSequence(
                cmd.getActivityId(), phase, resolvedSequence - 1);
        if (candidates.isEmpty()) {
            throw TaskOrderException.previousMissing(cmd.getActivityId(), phase.name(), resolvedSequence - 1);
        }
        if (candidates.size() > 1) {
            throw TaskOrderException.previousAmbiguous(cmd.getActivityId(), phase.name(), resolvedSequence - 1, candidates.size());
        }
        return candidates.get(0).getId().getValue();
    }

    /**
     * 显式前序校验（IOV-OTA-DSN-CR-017 §3.3）
     * <p>前序必须存在、不得指向自身、属于同一 Activity；同 phase 时 sequence 必须小于当前；
     * 跨 phase 时必须满足 US-054 阶段顺序，且不得借此绕过跳阶审批。</p>
     */
    private void validateExplicitPrevious(Task prev, Long activityId, TaskPhase phase,
                                          long resolvedSequence, Long newTaskId) {
        if (prev.getId().getValue().equals(newTaskId)) {
            throw TaskOrderException.previousScopeMismatch("前序任务不能指向自身");
        }
        if (!prev.getActivityId().getValue().equals(activityId)) {
            throw TaskOrderException.previousScopeMismatch(
                    "前序任务[" + prev.getId().getValue() + "]不属于同一升级活动，无法作为放行依据");
        }
        if (prev.getPhase() == phase) {
            int prevSeq = prev.getSequenceNo() != null ? prev.getSequenceNo() : -1;
            if (prevSeq >= resolvedSequence) {
                throw TaskOrderException.previousScopeMismatch(
                        "同阶段前序任务序号[" + prevSeq + "]必须小于当前任务序号[" + resolvedSequence + "]");
            }
        } else {
            // 跨 phase：必须满足 US-054 阶段顺序（前序阶段先于当前阶段）
            if (prev.getPhase().getValue() >= phase.getValue()) {
                throw TaskOrderException.previousScopeMismatch(
                        "跨阶段前序任务[" + prev.getId().getValue() + "]阶段[" + prev.getPhase().name()
                                + "]不满足 US-054 阶段顺序，禁止作为前序");
            }
        }
    }

    /**
     * 补充任务只读展示字段（IOV-OTA-DSN-CR-017 §6.2）
     * <p>previousTaskName / previousPhase / previousReportState / releaseGateState；
     * 历史数据 previousTaskId 为 NULL 时保持只读展示原值，不写回、不猜测。</p>
     */
    private TaskResult enrichTaskResult(TaskResult result) {
        if (result.getTaskId() != null) {
            Optional<TaskReleaseGate> gate = taskReleaseGateRepository.getByNextTaskId(result.getTaskId());
            if (gate != null && gate.isPresent() && gate.get().getGateState() != null) {
                result.setReleaseGateState(gate.get().getGateState().getValue());
            }
        }
        if (result.getPreviousTaskId() != null) {
            Optional<Task> prevOpt = taskRepository.getById(TaskId.of(result.getPreviousTaskId()));
            if (prevOpt != null && prevOpt.isPresent()) {
                Task prev = prevOpt.get();
                result.setPreviousTaskName(prev.getName());
                result.setPreviousPhase(prev.getPhase() != null ? prev.getPhase().name() : null);
                Optional<TaskReport> reportOpt = taskReportRepository.findLatestByTaskId(prev.getId().getValue());
                result.setPreviousReportState(reportOpt != null && reportOpt.isPresent() ? "REPORTED" : "NONE");
            }
        }
        return result;
    }

    /**
     * 校验任务时间窗完全落在活动计划窗口内
     *
     * @param activityId 活动ID
     * @param taskStart  任务开始时间
     * @param taskEnd    任务结束时间
     */
    private void validateTaskWindowWithinActivity(Long activityId, java.time.Instant taskStart, java.time.Instant taskEnd) {
        if (activityId == null || taskStart == null || taskEnd == null) {
            return;
        }
        net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ActivityPo activity =
                activityAppService.getActivityById(activityId);
        if (activity == null) {
            return;
        }
        if (activity.getStartTime() != null && taskStart.isBefore(activity.getStartTime().toInstant())) {
            throw new IllegalStateException("任务开始时间不能早于活动计划窗口开始时间");
        }
        if (activity.getEndTime() != null && taskEnd.isAfter(activity.getEndTime().toInstant())) {
            throw new IllegalStateException("任务结束时间不能晚于活动计划窗口结束时间");
        }
    }
}