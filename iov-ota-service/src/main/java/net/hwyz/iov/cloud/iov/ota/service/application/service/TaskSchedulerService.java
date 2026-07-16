package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 任务调度服务
 * 负责定时检查已排程任务，在到达release_time后自动发布
 * 设计文档：US-065 立即/定时发布与取消排程
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerService {

    private final TaskAppService taskAppService;
    private final TaskRepository taskRepository;

    /**
     * 每分钟检查一次已排程任务，到达release_time后自动发布
     * 使用数据库扫描作为兜底机制
     * 设计文档要求：按 state=SCHEDULED AND release_time<=now 扫描
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoReleaseScheduledTasks() {
        log.debug("开始检查已排程任务...");
        
        List<Task> scheduledTasks = taskRepository.findScheduledTasks();
        if (scheduledTasks.isEmpty()) {
            log.debug("没有已排程的任务");
            return;
        }
        
        Instant now = Instant.now();
        int releasedCount = 0;
        int failedCount = 0;
        
        for (Task task : scheduledTasks) {
            try {
                // 检查是否到达release_time
                if (task.getReleaseTime() != null && !now.isBefore(task.getReleaseTime())) {
                    log.info("任务[{}]已到达计划发布时间[{}]，自动发布", task.getId().getValue(), task.getReleaseTime());
                    
                    // 使用统一发布事务
                    taskAppService.releaseTaskByScheduler(task.getId().getValue());
                    
                    releasedCount++;
                    log.info("任务[{}]自动发布成功", task.getId().getValue());
                }
            } catch (Exception e) {
                failedCount++;
                log.error("任务[{}]自动发布失败", task.getId().getValue(), e);
                
                // 记录失败摘要
                try {
                    Task taskForError = taskRepository.getById(TaskId.of(task.getId().getValue())).orElse(null);
                    if (taskForError != null) {
                        taskForError.setLastScheduleError(e.getMessage());
                        taskRepository.save(taskForError);
                    }
                } catch (Exception ex) {
                    log.error("记录任务[{}]调度失败信息失败", task.getId().getValue(), ex);
                }
            }
        }
        
        if (releasedCount > 0 || failedCount > 0) {
            log.info("本次检查完成，自动发布成功[{}]个任务，失败[{}]个任务", releasedCount, failedCount);
        } else {
            log.debug("本次检查完成，没有任务需要自动发布");
        }
    }
}
