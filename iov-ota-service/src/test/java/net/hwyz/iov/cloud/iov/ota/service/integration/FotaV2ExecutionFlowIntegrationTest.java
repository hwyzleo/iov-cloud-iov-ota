package net.hwyz.iov.cloud.iov.ota.service.integration;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionEventCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionFinalizeCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.RecoveryQueryCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionCreateResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionEventResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionFinalizeResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.RecoveryResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionEventAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.RecoveryAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.OutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.Vin;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.InstallPermitService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlAckMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEcuResultMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEventMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEventPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.security.LocalPermitTokenSigner;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.integration.support.InMemoryVehicleTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * CR-012 端到端集成测试：安装许可 → 事件 → 收口 → 恢复（T-6.1/6.2/6.3）
 *
 * <p>使用内存版领域仓库 + 真实应用服务，验证 Task → VehicleTask → Execution 三层运行模型全链路。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CR-012 集成测试 - Execution 全生命周期")
class FotaV2ExecutionFlowIntegrationTest {

    @Mock private ExecutionEcuResultMapper executionEcuResultMapper;
    @Mock private ExecutionEventMapper executionEventMapper;
    @Mock private ExecutionControlMapper executionControlMapper;
    @Mock private ExecutionControlAckMapper executionControlAckMapper;

    private InMemoryVehicleTaskRepository vehicleTaskRepository;
    private InMemoryExecutionRepository executionRepository;
    private InMemoryTaskRepository taskRepository;
    private InMemoryOutboxRepository outboxRepository;

    private ExecutionAppService executionAppService;
    private ExecutionEventAppService executionEventAppService;
    private RecoveryAppService recoveryAppService;

    private final Map<String, ExecutionEventPo> eventStore = new HashMap<>();

    private Task task;
    private VehicleTask vehicleTask;
    private final Instant now = Instant.now();
    private final Instant startTime = now.minusSeconds(60);
    private final Instant endTime = now.plusSeconds(3600);

    @BeforeEach
    void setUp() {
        vehicleTaskRepository = new InMemoryVehicleTaskRepository();
        executionRepository = new InMemoryExecutionRepository();
        taskRepository = new InMemoryTaskRepository();
        outboxRepository = new InMemoryOutboxRepository();

        // 内存事件存储
        when(executionEventMapper.selectByEventId(any())).thenAnswer(inv -> eventStore.get(inv.getArgument(0)));
        when(executionEventMapper.insert(any(ExecutionEventPo.class))).thenAnswer(inv -> {
            ExecutionEventPo po = inv.getArgument(0);
            eventStore.put(po.getEventId(), po);
            return 1;
        });
        when(executionControlMapper.selectLatestByExecutionId(any())).thenReturn(null);

        // 领域服务
        InstallPermitService installPermitService = new InstallPermitService(new LocalPermitTokenSigner());

        // 应用服务
        executionAppService = new ExecutionAppService(vehicleTaskRepository, executionRepository,
                taskRepository, installPermitService, outboxRepository, executionEcuResultMapper);
        executionEventAppService = new ExecutionEventAppService(executionRepository,
                executionEventMapper, executionControlMapper, executionControlAckMapper);
        recoveryAppService = new RecoveryAppService(vehicleTaskRepository, executionRepository,
                executionControlMapper);

        // 初始状态：任务已发布、车辆任务就绪可安装
        task = buildReleasedTask();
        vehicleTask = buildReadyVehicleTask();
        taskRepository.save(task);
        vehicleTaskRepository.save(vehicleTask);
    }

    @Test
    @DisplayName("完整链路：申请安装 → 事件 → 收口成功 → 恢复一致")
    void fullExecutionLifecycle() {
        // 1. 申请安装许可
        ExecutionCreateCmd createCmd = ExecutionCreateCmd.builder()
                .vehicleTaskId(vehicleTask.getId().getValue())
                .idempotencyKey("idem-flow-001")
                .installPlanVersion("PLAN_V1")
                .packageManifestDigest("manifest-digest")
                .conditionSetVersion("COND_V1")
                .build();
        ExecutionCreateResult createResult = executionAppService.requestInstall(createCmd);

        assertEquals(1, createResult.getAttemptNo());
        assertNotNull(createResult.getPermitToken());
        assertEquals(VehicleTaskStatus.EXECUTING, vehicleTask.getStatus());
        Long executionId = createResult.getExecutionId();

        // 2. 接收连续事件
        for (long seq = 1; seq <= 3; seq++) {
            ExecutionEventResult eventResult = executionEventAppService.receiveEvent(
                    ExecutionEventCmd.builder()
                            .executionId(executionId)
                            .eventId("evt-flow-" + seq)
                            .sequenceNo(seq)
                            .eventType("PROGRESS")
                            .build());
            assertEquals("ACCEPTED", eventResult.getDisposition());
            assertEquals(seq, eventResult.getAcceptedSequenceNo());
        }

        // 3. 收口
        ExecutionFinalizeCmd finalizeCmd = ExecutionFinalizeCmd.builder()
                .executionId(executionId)
                .finalStatus("SUCCEEDED")
                .finalSequenceNo(3L)
                .resultDigest("result-digest")
                .build();
        ExecutionFinalizeResult finalizeResult = executionAppService.finalizeExecution(finalizeCmd);

        assertTrue(finalizeResult.isResultAccepted());
        assertEquals(ExecutionStatus.SUCCEEDED.getValue(), finalizeResult.getExecutionStatus());
        assertEquals(VehicleTaskStatus.SUCCEEDED.getValue(), finalizeResult.getVehicleTaskStatus());
        assertEquals(VehicleTaskStatus.SUCCEEDED, vehicleTask.getStatus());

        // 4. 恢复查询：一致
        RecoveryResult recovery = recoveryAppService.query(RecoveryQueryCmd.builder()
                .scope("EXECUTION").executionId(executionId).build());
        assertEquals(ExecutionStatus.SUCCEEDED.getValue(), recovery.getExecutionStatus());
        assertEquals("CLOSED", recovery.getRecoveryAction());
    }

    @Test
    @DisplayName("乱序事件 BUFFERED 不提前推进状态，连续后可收口")
    void outOfOrderEvents_doNotAdvanceStatePrematurely() {
        ExecutionCreateResult createResult = requestInstall();
        Long executionId = createResult.getExecutionId();

        // 乱序：先收 3，水位仍 0
        ExecutionEventResult buffered = executionEventAppService.receiveEvent(
                ExecutionEventCmd.builder().executionId(executionId).eventId("evt-3").sequenceNo(3L).build());
        assertEquals("BUFFERED", buffered.getDisposition());
        assertEquals(0L, buffered.getAcceptedSequenceNo());
        assertFalse(buffered.getMissingSequenceRanges().isEmpty());

        // 补齐 1、2、3，水位推进到 3
        for (long seq = 1; seq <= 3; seq++) {
            executionEventAppService.receiveEvent(
                    ExecutionEventCmd.builder().executionId(executionId).eventId("evt-fill-" + seq).sequenceNo(seq).build());
        }

        ExecutionFinalizeResult finalize = executionAppService.finalizeExecution(
                ExecutionFinalizeCmd.builder().executionId(executionId)
                        .finalStatus("SUCCEEDED").finalSequenceNo(3L).build());
        assertTrue(finalize.isResultAccepted());
    }

    @Test
    @DisplayName("并发/重复申请：已有活动 Execution 时拒绝新申请（不递增 attemptNo）")
    void requestInstall_activeExecution_conflict() {
        // 首次申请成功
        ExecutionCreateResult first = requestInstall();
        assertEquals(1, first.getAttemptNo());
        assertTrue(executionRepository.findActiveByVehicleTaskId(vehicleTask.getId()).isPresent());

        // 第二次申请：VehicleTask 已 EXECUTING，InstallPermitService 拒绝
        assertThrows(net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException.class,
                this::requestInstall);
    }

    @Test
    @DisplayName("收口时水位未达最终序号：resultAccepted=false 并返回缺失范围")
    void finalize_watermarkGap_resultNotAccepted() {
        ExecutionCreateResult createResult = requestInstall();
        Long executionId = createResult.getExecutionId();

        // 只接收 1、3，缺 2
        executionEventAppService.receiveEvent(
                ExecutionEventCmd.builder().executionId(executionId).eventId("evt-1").sequenceNo(1L).build());
        executionEventAppService.receiveEvent(
                ExecutionEventCmd.builder().executionId(executionId).eventId("evt-3").sequenceNo(3L).build());

        ExecutionFinalizeResult finalize = executionAppService.finalizeExecution(
                ExecutionFinalizeCmd.builder().executionId(executionId)
                        .finalStatus("SUCCEEDED").finalSequenceNo(3L).build());

        assertFalse(finalize.isResultAccepted());
        assertNotNull(finalize.getMissingSequenceRanges());
        // 补齐 2 后可收口
        executionEventAppService.receiveEvent(
                ExecutionEventCmd.builder().executionId(executionId).eventId("evt-2").sequenceNo(2L).build());
        ExecutionFinalizeResult finalize2 = executionAppService.finalizeExecution(
                ExecutionFinalizeCmd.builder().executionId(executionId)
                        .finalStatus("SUCCEEDED").finalSequenceNo(3L).build());
        assertTrue(finalize2.isResultAccepted());
    }

    @Test
    @DisplayName("Outbox 记录安装创建与收口事件")
    void outbox_recordsExecutionEvents() {
        ExecutionCreateResult createResult = requestInstall();
        long outboxAfterCreate = outboxRepository.count();
        assertEquals(1L, outboxAfterCreate);

        // 收口后追加
        executionEventAppService.receiveEvent(
                ExecutionEventCmd.builder().executionId(createResult.getExecutionId()).eventId("evt-1").sequenceNo(1L).build());
        executionAppService.finalizeExecution(
                ExecutionFinalizeCmd.builder().executionId(createResult.getExecutionId())
                        .finalStatus("SUCCEEDED").finalSequenceNo(1L).build());
        assertEquals(2L, outboxRepository.count());
    }

    // ==================== 辅助 ====================

    private ExecutionCreateResult requestInstall() {
        return executionAppService.requestInstall(ExecutionCreateCmd.builder()
                .vehicleTaskId(vehicleTask.getId().getValue())
                .idempotencyKey("idem-" + System.nanoTime())
                .installPlanVersion("PLAN_V1")
                .packageManifestDigest("manifest-digest")
                .conditionSetVersion("COND_V1")
                .build());
    }

    private Task buildReleasedTask() {
        Task t = Task.create(TaskId.of(100L), "集成任务", TaskType.NORMAL, ActivityId.of(1L));
        t.setStartTime(startTime);
        t.setEndTime(endTime);
        t.setMinimumProtocolVersion("2.0");
        t.submit();
        t.approve(true, null);
        t.release(Set.of(Vin.of("VIN001")), "IMMEDIATE");
        t.setReleaseTime(now.minusSeconds(120));
        return t;
    }

    private VehicleTask buildReadyVehicleTask() {
        VehicleTask vt = VehicleTask.create(
                VehicleTaskId.of(10L), 100L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("digest"),
                now.minusSeconds(120), startTime, endTime);
        vt.markVisible(now);
        vt.enterConsentPending();
        vt.grantConsent(false);
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vt.getStatus());
        return vt;
    }
}
