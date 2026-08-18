package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskMetricResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.PhaseGatePolicyRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskMetricRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CR-015 任务健康指标查询服务测试
 * <p>指标以 tb_task_vehicle / tb_task_vehicle_execution 权威状态聚合并快照。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskMetricQueryService 指标聚合")
class TaskMetricQueryServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskVehicleMapper taskVehicleMapper;
    @Mock private TaskMetricRepository taskMetricRepository;
    @Mock private PhaseGatePolicyRepository phaseGatePolicyRepository;

    @InjectMocks
    private TaskMetricQueryService service;

    private Task task;

    @BeforeEach
    void setUp() {
        task = Task.create(TaskId.of(1L), "任务A", TaskType.NORMAL, ActivityId.of(100L));
        task.setPhase(TaskPhase.CANARY);
        task.setState(TaskState.IN_PROGRESS);
    }

    private void stubCounts() {
        when(taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(eq(1L), eq("SUCCEEDED"))).thenReturn(90);
        when(taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(eq(1L), eq("FAILED"))).thenReturn(5);
        when(taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(eq(1L), eq("ROLLED_BACK"))).thenReturn(3);
        when(taskVehicleMapper.countTimeoutExecutionByTaskId(1L)).thenReturn(2);
        when(taskVehicleMapper.countAllByTaskId(1L)).thenReturn(100);
    }

    @Test
    @DisplayName("聚合成功率/失败率/完成率并快照 tb_task_metric")
    void aggregateAndSnapshot() {
        when(taskRepository.getById(any())).thenReturn(Optional.of(task));
        stubCounts();
        when(phaseGatePolicyRepository.findByPhaseAndActivity(TaskPhase.CANARY.getValue(), 100L))
                .thenReturn(Optional.empty());

        TaskMetricResult result = service.getMetric(1L);

        assertEquals(90, result.getSuccessCnt());
        assertEquals(8, result.getFailCnt());
        assertEquals(2, result.getTimeoutCnt());
        assertEquals(100, result.getTotalCnt());
        assertEquals("OK", result.getGateState());
        verify(taskMetricRepository).save(any());
    }

    @Test
    @DisplayName("成功率低于门禁阈值 -> gateState=BREACH")
    void belowThreshold_breach() {
        when(taskRepository.getById(any())).thenReturn(Optional.of(task));
        stubCounts();
        PhaseGatePolicy policy = PhaseGatePolicy.builder()
                .phase(TaskPhase.CANARY)
                .activityId(100L)
                .successRateMin(BigDecimal.valueOf(0.98))
                .build();
        when(phaseGatePolicyRepository.findByPhaseAndActivity(TaskPhase.CANARY.getValue(), 100L))
                .thenReturn(Optional.of(policy));

        TaskMetricResult result = service.getMetric(1L);

        assertEquals("BREACH", result.getGateState());
        assertEquals(BigDecimal.valueOf(0.98), result.getGateThreshold());
    }
}
