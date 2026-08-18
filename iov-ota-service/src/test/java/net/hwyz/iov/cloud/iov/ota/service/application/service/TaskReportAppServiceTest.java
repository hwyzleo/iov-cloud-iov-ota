package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskReportResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CR-015 任务报告应用服务测试：终态不可变正式报告 + provisional 统计
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskReportAppService 报告生成与查询")
class TaskReportAppServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskReportRepository taskReportRepository;
    @Mock private TaskVehicleMapper taskVehicleMapper;

    @InjectMocks
    private TaskReportAppService service;

    private Task terminalTask;

    @BeforeEach
    void setUp() {
        terminalTask = Task.create(TaskId.of(1L), "任务A", TaskType.NORMAL, ActivityId.of(100L));
        terminalTask.setState(TaskState.COMPLETED);
        terminalTask.setSequenceNo(1);
    }

    private void stubCounts() {
        when(taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(anyLong(), eq("SUCCEEDED"))).thenReturn(90);
        when(taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(anyLong(), eq("FAILED"))).thenReturn(5);
        when(taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(anyLong(), eq("ROLLED_BACK"))).thenReturn(3);
        when(taskVehicleMapper.countTimeoutExecutionByTaskId(anyLong())).thenReturn(2);
        when(taskVehicleMapper.countAllByTaskId(anyLong())).thenReturn(100);
    }

    @Test
    @DisplayName("终态无既有报告 -> 生成 reportVersion=1 的正式报告")
    void terminal_generatesFormalReport() {
        when(taskRepository.getById(any())).thenReturn(Optional.of(terminalTask));
        when(taskReportRepository.findLatestByTaskId(1L)).thenReturn(Optional.empty());
        when(taskReportRepository.listByTaskId(1L)).thenReturn(List.of());
        stubCounts();

        TaskReport report = service.generateFormalReport(1L);

        assertNotNull(report);
        assertEquals(1, report.getReportVersion());
        assertTrue(report.getSuccessRate().compareTo(BigDecimal.valueOf(0.9)) >= 0);
        verify(taskReportRepository).save(any(TaskReport.class));
    }

    @Test
    @DisplayName("终态已有正式报告 -> 幂等，不覆盖")
    void terminal_existingReport_idempotent() {
        when(taskRepository.getById(any())).thenReturn(Optional.of(terminalTask));
        TaskReport existing = new TaskReport().setId(10L).setTaskId(1L).setReportVersion(1);
        when(taskReportRepository.findLatestByTaskId(1L)).thenReturn(Optional.of(existing));

        TaskReport report = service.generateFormalReport(1L);

        assertSame(existing, report);
        verify(taskReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("执行中查询 -> provisional=true，不返回正式报告")
    void running_provisional() {
        Task running = Task.create(TaskId.of(2L), "任务B", TaskType.NORMAL, ActivityId.of(100L));
        running.setState(TaskState.IN_PROGRESS);
        when(taskRepository.getById(any())).thenReturn(Optional.of(running));
        stubCounts();

        TaskReportResult result = service.getReport(2L);

        assertTrue(result.isProvisional());
        assertNull(result.getReportVersion());
        assertEquals("IN_PROGRESS", result.getTaskState());
        verify(taskReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("终态查询 -> 返回正式报告，provisional=false")
    void terminal_queryFormal() {
        when(taskRepository.getById(any())).thenReturn(Optional.of(terminalTask));
        TaskReport formal = new TaskReport().setId(10L).setTaskId(1L).setReportVersion(1)
                .setSuccessRate(BigDecimal.valueOf(0.9));
        when(taskReportRepository.findLatestByTaskId(1L)).thenReturn(Optional.of(formal));

        TaskReportResult result = service.getReport(1L);

        assertFalse(result.isProvisional());
        assertEquals(1, result.getReportVersion());
    }

    @Test
    @DisplayName("非终态任务不生成正式报告")
    void nonTerminal_noFormalReport() {
        Task draft = Task.create(TaskId.of(3L), "任务C", TaskType.NORMAL, ActivityId.of(100L));
        when(taskRepository.getById(any())).thenReturn(Optional.of(draft));

        assertNull(service.generateFormalReport(3L));
        verify(taskReportRepository, never()).save(any());
    }
}
