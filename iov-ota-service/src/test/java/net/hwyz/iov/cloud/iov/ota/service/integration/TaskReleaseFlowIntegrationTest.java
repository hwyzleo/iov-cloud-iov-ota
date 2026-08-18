package net.hwyz.iov.cloud.iov.ota.service.integration;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskReleaseGateResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskReleaseGateService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskReportAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.TaskReleaseGateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.PhaseGatePolicyRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReleaseGateRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TaskReleaseGateDomainService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleMapper;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryPhaseGatePolicyRepository;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryTaskReleaseGateRepository;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryTaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * CR-015 P0 集成测试：多任务放量门禁与任务报告全链路（对应 §8 验收）
 * <p>内存版领域仓储 + 真实应用服务，验证：
 * 无前序直接放行、前序正式报告 PASS 放行、无报告 PENDING 拦截、非同活动拒绝、
 * 人工 override 放行、正式报告幂等。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CR-015 P0 集成测试 - 多任务放量门禁")
class TaskReleaseFlowIntegrationTest {

    @Mock private TaskVehicleMapper taskVehicleMapper;

    private TaskRepository taskRepository;
    private TaskReportRepository taskReportRepository;
    private TaskReleaseGateRepository taskReleaseGateRepository;
    private PhaseGatePolicyRepository phaseGatePolicyRepository;
    private TaskReleaseGateService taskReleaseGateService;
    private TaskReportAppService taskReportAppService;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepository();
        taskReportRepository = new InMemoryTaskReportRepository();
        taskReleaseGateRepository = new InMemoryTaskReleaseGateRepository();
        phaseGatePolicyRepository = new InMemoryPhaseGatePolicyRepository();

        taskReportAppService = new TaskReportAppService(taskRepository, taskReportRepository, taskVehicleMapper);
        taskReleaseGateService = new TaskReleaseGateService(
                taskRepository, taskReportRepository, taskReleaseGateRepository,
                phaseGatePolicyRepository, new TaskReleaseGateDomainService());
    }

    private Task waveTask(long id, long activityId, Integer sequenceNo, Long prevTaskId, TaskPhase phase) {
        Task task = Task.create(TaskId.of(id), "波次" + id, TaskType.NORMAL, ActivityId.of(activityId));
        task.setSequenceNo(sequenceNo);
        task.setPreviousTaskId(prevTaskId);
        task.setPhase(phase);
        task.setState(TaskState.APPROVED);
        taskRepository.save(task);
        return task;
    }

    private void stubReportCounts() {
        when(taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(anyLong(), eq("SUCCEEDED"))).thenReturn(95);
        when(taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(anyLong(), eq("FAILED"))).thenReturn(3);
        when(taskVehicleMapper.countByTaskIdAndVehicleTaskStatus(anyLong(), eq("ROLLED_BACK"))).thenReturn(1);
        when(taskVehicleMapper.countTimeoutExecutionByTaskId(anyLong())).thenReturn(1);
        when(taskVehicleMapper.countAllByTaskId(anyLong())).thenReturn(100);
    }

    @Test
    @DisplayName("无前序任务的波次 -> 直接放行")
    void noPreviousTask_release() {
        waveTask(1L, 100L, 1, null, TaskPhase.VALIDATION);
        assertEquals(ReleaseGateState.PASS, taskReleaseGateService.checkGateForRelease(1L));
    }

    @Test
    @DisplayName("前序终态生成正式报告后 -> 下一波次 PASS 放行")
    void prevFormalReport_thenNextRelease() {
        // 波次1（无前序）发布
        waveTask(1L, 100L, 1, null, TaskPhase.CANARY);
        assertEquals(ReleaseGateState.PASS, taskReleaseGateService.checkGateForRelease(1L));

        // 波次1 终态 + 生成正式报告
        Task wave1 = taskRepository.getById(TaskId.of(1L)).orElseThrow();
        wave1.setState(TaskState.COMPLETED);
        taskRepository.save(wave1);
        stubReportCounts();
        TaskReport formal = taskReportAppService.generateFormalReport(1L);
        assertNotNull(formal);
        assertEquals(1, formal.getReportVersion());

        // 配置门禁阈值策略（活动级）
        phaseGatePolicyRepository.save(PhaseGatePolicy.builder()
                .phase(TaskPhase.CANARY)
                .activityId(100L)
                .successRateMin(BigDecimal.valueOf(0.95))
                .failCntMax(5)
                .build());

        // 波次2（同活动，prev=1）发布 -> PASS
        waveTask(2L, 100L, 2, 1L, TaskPhase.CANARY);
        assertEquals(ReleaseGateState.PASS, taskReleaseGateService.checkGateForRelease(2L));

        // 波次1 对波次2 的放行结论为 PASS
        TaskReleaseGateResult gate = taskReleaseGateService.queryGateForTask(1L);
        assertEquals(ReleaseGateState.PASS.getValue(), gate.getGateState());
        assertEquals(2L, gate.getNextTaskId());
        assertEquals("1:1", gate.getReportRef());
    }

    @Test
    @DisplayName("前序无正式报告 -> PENDING 拦截；人工 override 后放行")
    void noReport_blockedThenOverride() {
        waveTask(1L, 100L, 1, null, TaskPhase.VALIDATION);
        waveTask(2L, 100L, 2, 1L, TaskPhase.VALIDATION);

        // 前序任务1 尚未终态/无正式报告 -> 拦截
        TaskReleaseGateException ex = assertThrows(TaskReleaseGateException.class,
                () -> taskReleaseGateService.checkGateForRelease(2L));
        assertTrue(ex.getMessage().contains("正式报告"));

        // 人工 override 放行
        TaskReleaseGateResult overridden = taskReleaseGateService.overrideGateForNextTask(2L, "ops", "APR-001", "人工放行");
        assertEquals(ReleaseGateState.PASS.getValue(), overridden.getGateState());
        assertTrue(overridden.getOverride());

        // 再次发布 -> 复用已放行门禁，PASS
        assertEquals(ReleaseGateState.PASS, taskReleaseGateService.checkGateForRelease(2L));
    }

    @Test
    @DisplayName("前序任务非同活动 -> 拒绝放行")
    void previousTaskDifferentActivity_rejected() {
        waveTask(1L, 100L, 1, null, TaskPhase.VALIDATION);
        // 波次2 属于活动 999，但 previousTaskId 指向活动 100 的任务1
        waveTask(2L, 999L, 1, 1L, TaskPhase.VALIDATION);

        assertThrows(TaskReleaseGateException.class, () -> taskReleaseGateService.checkGateForRelease(2L));
    }

    @Test
    @DisplayName("正式报告幂等：终态多次生成不覆盖，reportVersion 唯一")
    void formalReport_idempotent() {
        waveTask(1L, 100L, 1, null, TaskPhase.VALIDATION);
        Task wave1 = taskRepository.getById(TaskId.of(1L)).orElseThrow();
        wave1.setState(TaskState.CANCELED);
        taskRepository.save(wave1);
        stubReportCounts();

        TaskReport r1 = taskReportAppService.generateFormalReport(1L);
        TaskReport r2 = taskReportAppService.generateFormalReport(1L);
        assertEquals(1, r1.getReportVersion());
        assertSame(r1, r2); // 不覆盖
    }
}
