package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.query.TaskQuery;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskResult;
import net.hwyz.iov.cloud.iov.ota.service.application.assembler.TaskAssembler;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.event.publisher.DomainEventPublisher;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.TaskNotExistException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAppService {

    private final TaskRepository taskRepository;
    private final TaskAssembler taskAssembler;
    private final DomainEventPublisher eventPublisher;

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
            .map(taskAssembler::toResult)
            .collect(Collectors.toList());
    }

    public TaskResult getTaskById(Long id) {
        Task task = taskRepository.getById(TaskId.of(id))
            .orElseThrow(() -> new TaskNotExistException(id));
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult createTask(TaskCreateCmd cmd) {
        log.info("创建任务: {}", cmd.getName());
        
        Task task = Task.create(
            TaskId.of(generateId()),
            cmd.getName(),
            TaskType.valueOf(cmd.getType()),
            ActivityId.of(cmd.getActivityId())
        );
        task.setTarget(cmd.getTarget());
        task.setStartTime(cmd.getStartTime());
        task.setEndTime(cmd.getEndTime());
        task.setNoticeType(cmd.getNoticeType());
        task.setUpgradeMode(cmd.getUpgradeMode() != null ? 
            net.hwyz.iov.cloud.iov.ota.api.vo.enums.UpgradeMode.valueOf(cmd.getUpgradeMode()) : null);
        task.setDescription(cmd.getDescription());
        
        if (cmd.getRestrictions() != null) {
            task.loadRestrictionsAndStrategies(
                taskAssembler.toRestrictions(cmd.getRestrictions()),
                taskAssembler.toStrategies(cmd.getStrategies())
            );
        }
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult submitTask(TaskSubmitCmd cmd) {
        log.info("提交任务: {}", cmd.getTaskId());
        
        Task task = taskRepository.getById(TaskId.of(cmd.getTaskId()))
            .orElseThrow(() -> new TaskNotExistException(cmd.getTaskId()));
        
        taskAssembler.updateFromCmd(task, cmd);
        task.submit();
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult auditTask(TaskAuditCmd cmd) {
        log.info("审核任务: {}, 结果: {}", cmd.getTaskId(), cmd.getApproved());
        
        Task task = taskRepository.getById(TaskId.of(cmd.getTaskId()))
            .orElseThrow(() -> new TaskNotExistException(cmd.getTaskId()));
        
        task.audit(cmd.getApproved(), cmd.getReason());
        
        taskRepository.save(task);
        eventPublisher.publishAll(task.getPendingEvents());
        task.clearPendingEvents();
        
        return taskAssembler.toResult(task);
    }

    @Transactional
    public TaskResult releaseTask(Long taskId) {
        log.info("发布任务: {}", taskId);
        
        Task task = taskRepository.getById(TaskId.of(taskId))
            .orElseThrow(() -> new TaskNotExistException(taskId));
        
        task.release();
        
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
        
        return taskAssembler.toResult(task);
    }

    public int deleteTaskByIds(Long[] ids) {
        List<TaskId> taskIdList = List.of(ids).stream()
            .map(TaskId::of)
            .collect(Collectors.toList());
        taskRepository.deleteAll(taskIdList);
        return ids.length;
    }

    private Long generateId() {
        return System.currentTimeMillis();
    }
}