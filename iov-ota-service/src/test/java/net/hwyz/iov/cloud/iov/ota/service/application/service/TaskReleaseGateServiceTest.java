package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ReleaseGateState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.TaskReleaseGateResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.TaskReleaseGateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.PhaseGatePolicy;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReleaseGate;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskReport;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.PhaseGatePolicyRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReleaseGateRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskReportRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.TaskReleaseGateDomainService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CR-015 多任务放行门禁应用服务测试
 * <p>验收：前序正式报告缺失/FAIL 拦截、PASS 放行、非同活动拒绝、override 审计。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskReleaseGateService 放行门禁")
class TaskReleaseGateServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskReportRepository taskReportRepository;
    @Mock private TaskReleaseGateRepository taskReleaseGateRepository;
    @Mock private PhaseGatePolicyRepository phaseGatePolicyRepository;
    @Mock private TaskReleaseGateDomainService taskReleaseGateDomainService;

    @InjectMocks
    private TaskReleaseGateService service;

    private Task nextTask;
    private Task prevTask;

    @BeforeEach
    void setUp() {
        nextTask = Task.create(TaskId.of(2L), "波次2", TaskType.NORMAL, ActivityId.of(100L));
        nextTask.setSequenceNo(2);
        nextTask.setPreviousTaskId(1L);
        nextTask.setPhase(TaskPhase.CANARY);
        nextTask.setState(TaskState.APPROVED);

        prevTask = Task.create(TaskId.of(1L), "波次1", TaskType.NORMAL, ActivityId.of(100L));
        prevTask.setSequenceNo(1);
        prevTask.setPhase(TaskPhase.CANARY);
        prevTask.setState(TaskState.COMPLETED);
    }

    @Test
    @DisplayName("VALIDATION 阶段首波（seq=0 且无前序）-> 按既有首阶段规则放行 PASS")
    void validationFirstWave_pass() {
        nextTask.setSequenceNo(0);
        nextTask.setPreviousTaskId(null);
        nextTask.setPhase(TaskPhase.VALIDATION);
        when(taskRepository.getById(any())).thenReturn(Optional.of(nextTask));
        assertEquals(ReleaseGateState.PASS, service.checkGateForRelease(2L));
    }

    @Test
    @DisplayName("后续波次缺失前序关系（seq>0 且 previousTaskId 为空）-> fail-safe 阻断")
    void laterWaveMissingRelation_blocked() {
        nextTask.setSequenceNo(2);
        nextTask.setPreviousTaskId(null);
        when(taskRepository.getById(any())).thenReturn(Optional.of(nextTask));

        TaskReleaseGateException ex = assertThrows(TaskReleaseGateException.class,
                () -> service.checkGateForRelease(2L));
        assertTrue(ex.getMessage().contains("前序任务关系"));
    }

    @Test
    @DisplayName("CANARY 首波（seq=0 且无前序）但前序阶段无任务 -> 按 US-054 阻断")
    void canaryFirstWaveWithoutPrevPhase_blocked() {
        nextTask.setSequenceNo(0);
        nextTask.setPreviousTaskId(null);
        nextTask.setPhase(TaskPhase.CANARY);
        when(taskRepository.getById(any())).thenReturn(Optional.of(nextTask));
        when(taskRepository.findByActivityId(any())).thenReturn(java.util.List.of());

        TaskReleaseGateException ex = assertThrows(TaskReleaseGateException.class,
                () -> service.checkGateForRelease(2L));
        assertTrue(ex.getMessage().contains("前序阶段"));
    }

    @Test
    @DisplayName("前序任务非同活动 -> 拦截并抛异常")
    void previousTaskDifferentActivity_blocked() {
        Task otherActivityPrev = Task.create(TaskId.of(1L), "波次1", TaskType.NORMAL, ActivityId.of(999L));
        when(taskRepository.getById(TaskId.of(2L))).thenReturn(Optional.of(nextTask));
        when(taskRepository.getById(TaskId.of(1L))).thenReturn(Optional.of(otherActivityPrev));

        assertThrows(TaskReleaseGateException.class, () -> service.checkGateForRelease(2L));
    }

    @Test
    @DisplayName("前序无正式报告 -> PENDING 拦截并记录门禁")
    void previousTaskNoReport_pendingBlocked() {
        when(taskRepository.getById(TaskId.of(2L))).thenReturn(Optional.of(nextTask));
        when(taskRepository.getById(TaskId.of(1L))).thenReturn(Optional.of(prevTask));
        when(taskReleaseGateRepository.getByNextTaskId(2L)).thenReturn(Optional.empty());
        when(taskReportRepository.findLatestByTaskId(1L)).thenReturn(Optional.empty());

        TaskReleaseGateException ex = assertThrows(TaskReleaseGateException.class,
                () -> service.checkGateForRelease(2L));
        assertTrue(ex.getMessage().contains("正式报告"));

        // 应记录一条 PENDING 门禁
        verify(taskReleaseGateRepository).save(argThat(g -> g.getGateState() == ReleaseGateState.PENDING));
    }

    @Test
    @DisplayName("前序正式报告满足阈值 -> PASS 放行并记录门禁")
    void formalReportPass_release() {
        when(taskRepository.getById(TaskId.of(2L))).thenReturn(Optional.of(nextTask));
        when(taskRepository.getById(TaskId.of(1L))).thenReturn(Optional.of(prevTask));
        when(taskRepository.getById(TaskId.of(1L))).thenReturn(Optional.of(prevTask));
        when(taskReleaseGateRepository.getByNextTaskId(2L)).thenReturn(Optional.empty());

        TaskReport report = new TaskReport().setTaskId(1L).setReportVersion(1)
                .setSuccessRate(BigDecimal.valueOf(0.98))
                .setFailCaseDist("{\"SUCCEEDED\":98,\"FAILED\":1,\"ROLLED_BACK\":0,\"TIMED_OUT\":1}");
        when(taskReportRepository.findLatestByTaskId(1L)).thenReturn(Optional.of(report));

        PhaseGatePolicy policy = PhaseGatePolicy.builder()
                .phase(TaskPhase.CANARY)
                .activityId(100L)
                .successRateMin(BigDecimal.valueOf(0.95))
                .failCntMax(3)
                .build();
        when(phaseGatePolicyRepository.findByPhaseAndActivity(TaskPhase.CANARY.getValue(), 100L))
                .thenReturn(Optional.of(policy));
        when(taskReleaseGateDomainService.evaluate(policy, report.getSuccessRate(), 1))
                .thenReturn(ReleaseGateState.PASS);
        when(taskReleaseGateDomainService.toThresholdSnapshot(policy)).thenReturn("{}");

        assertEquals(ReleaseGateState.PASS, service.checkGateForRelease(2L));
        verify(taskReleaseGateRepository).save(argThat(g ->
                g.getGateState() == ReleaseGateState.PASS
                        && TaskReleaseGate.ReleaseGateType.SAME_PHASE == g.getGateType()
                        && "1:1".equals(g.getReportRef())));
    }

    @Test
    @DisplayName("前序正式报告不满足阈值 -> FAIL 拦截")
    void formalReportFail_blocked() {
        when(taskRepository.getById(TaskId.of(2L))).thenReturn(Optional.of(nextTask));
        when(taskRepository.getById(TaskId.of(1L))).thenReturn(Optional.of(prevTask));
        when(taskReleaseGateRepository.getByNextTaskId(2L)).thenReturn(Optional.empty());

        TaskReport report = new TaskReport().setTaskId(1L).setReportVersion(1)
                .setSuccessRate(BigDecimal.valueOf(0.80))
                .setFailCaseDist("{\"SUCCEEDED\":80,\"FAILED\":20,\"ROLLED_BACK\":0,\"TIMED_OUT\":0}");
        when(taskReportRepository.findLatestByTaskId(1L)).thenReturn(Optional.of(report));

        PhaseGatePolicy policy = PhaseGatePolicy.builder()
                .phase(TaskPhase.CANARY)
                .activityId(100L)
                .successRateMin(BigDecimal.valueOf(0.95))
                .failCntMax(3)
                .build();
        when(phaseGatePolicyRepository.findByPhaseAndActivity(TaskPhase.CANARY.getValue(), 100L))
                .thenReturn(Optional.of(policy));
        when(taskReleaseGateDomainService.evaluate(policy, report.getSuccessRate(), 20))
                .thenReturn(ReleaseGateState.FAIL);

        assertThrows(TaskReleaseGateException.class, () -> service.checkGateForRelease(2L));
    }

    @Test
    @DisplayName("已存在 PASS 门禁（含人工放行）-> 直接复用放行")
    void existingPassGate_reuse() {
        when(taskRepository.getById(TaskId.of(2L))).thenReturn(Optional.of(nextTask));
        when(taskRepository.getById(TaskId.of(1L))).thenReturn(Optional.of(prevTask));
        TaskReleaseGate gate = TaskReleaseGate.builder()
                .id(10L).activityId(100L).previousTaskId(1L).nextTaskId(2L)
                .gateType(TaskReleaseGate.ReleaseGateType.SAME_PHASE)
                .gateState(ReleaseGateState.PASS)
                .override(true)
                .build();
        when(taskReleaseGateRepository.getByNextTaskId(2L)).thenReturn(Optional.of(gate));

        assertEquals(ReleaseGateState.PASS, service.checkGateForRelease(2L));
        verify(taskReportRepository, never()).findLatestByTaskId(anyLong());
    }

    @Test
    @DisplayName("人工放行 override：门禁置 PASS 并固化审批引用/原因")
    void overrideGate() {
        TaskReleaseGate gate = TaskReleaseGate.builder()
                .id(10L).activityId(100L).previousTaskId(1L).nextTaskId(2L)
                .gateType(TaskReleaseGate.ReleaseGateType.SAME_PHASE)
                .gateState(ReleaseGateState.FAIL)
                .build();
        when(taskReleaseGateRepository.getByNextTaskId(2L)).thenReturn(Optional.of(gate));

        TaskReleaseGateResult result = service.overrideGateForNextTask(2L, "ops", "APR-009", "人工放行");

        assertEquals(ReleaseGateState.PASS.getValue(), result.getGateState());
        assertTrue(result.getOverride());
        assertEquals("APR-009", result.getApprovalRef());
        assertEquals("人工放行", result.getDescription());
        verify(taskReleaseGateRepository).save(gate);
    }

    @Test
    @DisplayName("查询某任务对下一任务的放行结论：无门禁时返回 PENDING")
    void queryGate_noGate_pending() {
        when(taskRepository.getById(any())).thenReturn(Optional.of(prevTask));
        when(taskReleaseGateRepository.getByPreviousTaskId(1L)).thenReturn(Optional.empty());

        TaskReleaseGateResult result = service.queryGateForTask(1L);

        assertEquals(ReleaseGateState.PENDING.getValue(), result.getGateState());
        assertEquals(1L, result.getPreviousTaskId());
    }
}
