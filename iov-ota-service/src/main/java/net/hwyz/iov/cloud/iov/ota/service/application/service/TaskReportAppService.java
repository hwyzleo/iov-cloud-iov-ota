package net.hwyz.iov.cloud.iov.ota.service.application.service;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskReportResult;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.TaskNotExistException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * 任务报告应用服务（CR-015 §3.2）
 * <p>任务进入终态时生成不可变正式报告（reportVersion 幂等，写后不可原地覆盖）；
 * 执行中查询返回 provisional=true，不得作为下一任务正式放行依据。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskReportAppService {

    private final TaskRepository taskRepository;
    private final TaskReportRepository taskReportRepository;
    private final TaskVehicleMapper taskVehicleMapper;

    /**
     * 查询任务报告：终态返回正式报告，执行中返回 provisional 统计
     */
    @Transactional(readOnly = true)
    public TaskReportResult getReport(Long taskId) {
        Task task = taskRepository.getById(TaskId.of(taskId))
                .orElseThrow(() -> new TaskNotExistException(taskId));

        if (task.isTerminal()) {
            TaskReport formal = taskReportRepository.findLatestByTaskId(taskId).orElse(null);
            if (formal != null) {
                return toResult(task, formal, false);
            }
            // 终态但尚未生成正式报告（兜底）：本次查询即生成
            TaskReport generated = generateFormalReport(taskId);
            return toResult(task, generated, false);
        }

        // 执行中：provisional 统计
        Stats stats = computeStats(taskId);
        return TaskReportResult.builder()
                .taskId(taskId)
                .reportVersion(null)
                .completeRate(stats.completeRate)
                .successRate(stats.successRate)
                .failCaseDist(stats.failCaseDist)
                .genTime(Instant.now())
                .provisional(true)
                .taskState(task.getState().name())
                .build();
    }

    /**
     * 终态生成不可变正式报告（幂等：已有正式报告则不覆盖）
     * @return 正式报告
     */
    @Transactional
    public TaskReport generateFormalReport(Long taskId) {
        Task task = taskRepository.getById(TaskId.of(taskId))
                .orElseThrow(() -> new TaskNotExistException(taskId));
        if (!task.isTerminal()) {
            log.info("任务[{}]未进入终态，不生成正式报告", taskId);
            return null;
        }

        TaskReport existing = taskReportRepository.findLatestByTaskId(taskId).orElse(null);
        if (existing != null) {
            log.info("任务[{}]已存在正式报告[reportVersion={}]，跳过生成", taskId, existing.getReportVersion());
            return existing;
        }

        Stats stats = computeStats(taskId);
        int reportVersion = taskReportRepository.listByTaskId(taskId).stream()
                .map(TaskReport::getReportVersion)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        TaskReport report = new TaskReport()
                .setTaskId(taskId)
                .setReportVersion(reportVersion)
                .setCompleteRate(stats.completeRate)
                .setSuccessRate(stats.successRate)
                .setFailCaseDist(stats.failCaseDist)
                .setGenTime(Instant.now());
        taskReportRepository.save(report);
        log.info("任务[{}]生成正式报告[reportVersion={}]", taskId, reportVersion);
        return report;
    }

    private TaskReportResult toResult(Task task, TaskReport report, boolean provisional) {
        return TaskReportResult.builder()
                .taskId(report.getTaskId())
                .reportVersion(report.getReportVersion())
                .completeRate(report.getCompleteRate())
                .successRate(report.getSuccessRate())
                .failCaseDist(report.getFailCaseDist())
                .genTime(report.getGenTime())
                .provisional(provisional)
                .taskState(task != null ? task.getState().name() : null)
                .build();
    }

    /**
     * 以 tb_task_vehicle / tb_task_vehicle_execution 权威状态聚合统计
     */
    private Stats computeStats(Long taskId) {
        int successCnt = taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(taskId, "SUCCEEDED");
        int failedCnt = taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(taskId, "FAILED");
        int rolledBackCnt = taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(taskId, "ROLLED_BACK");
        int timeoutCnt = taskVehicleMapper.countTimeoutExecutionByTaskId(taskId);
        int totalCnt = taskVehicleMapper.countAllByTaskId(taskId);

        int failCnt = failedCnt + rolledBackCnt;
        int decidedCnt = successCnt + failCnt + timeoutCnt;

        BigDecimal completeRate = totalCnt > 0
                ? BigDecimal.valueOf(successCnt + failCnt).divide(BigDecimal.valueOf(totalCnt), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal successRate = decidedCnt > 0
                ? BigDecimal.valueOf(successCnt).divide(BigDecimal.valueOf(decidedCnt), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        JSONObject dist = new JSONObject();
        dist.set("SUCCEEDED", successCnt);
        dist.set("FAILED", failedCnt);
        dist.set("ROLLED_BACK", rolledBackCnt);
        dist.set("TIMED_OUT", timeoutCnt);

        return new Stats(completeRate, successRate, dist.toString());
    }

    /** 统计结果内部载体 */
    private static class Stats {
        final BigDecimal completeRate;
        final BigDecimal successRate;
        final String failCaseDist;

        Stats(BigDecimal completeRate, BigDecimal successRate, String failCaseDist) {
            this.completeRate = completeRate;
            this.successRate = successRate;
            this.failCaseDist = failCaseDist;
        }
    }
}
