package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskPhase;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.TaskType;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.VehicleTaskResetCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskResetResult;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.VehicleTaskResetException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Task;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ActivityId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskResetSeed;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.TaskVehicleResetLogMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehicleResetLogPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 车辆任务重置应用服务测试（IOV-OTA-DSN-CR-025 §4.29.2 / §4.29.5 / §4.29.6）。
 *
 * <p>覆盖：状态准入矩阵、父任务守卫、时间窗、force 语义、当前性复核、并发冲突、
 * 新车辆任务初始化口径、审计字段与旧执行收口。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehicleTaskResetAppService 车辆任务重置")
class VehicleTaskResetAppServiceTest {

    private static final long VEHICLE_TASK_ID = 9001L;
    private static final long TASK_ID = 100L;
    private static final long ACTIVITY_ID = 7L;
    private static final long NEW_VEHICLE_TASK_ID = 9002L;
    private static final String VIN = "HWYZTEST900000001";

    @Mock private TaskRepository taskRepository;
    @Mock private VehicleTaskRepository vehicleTaskRepository;
    @Mock private TaskVehicleRepository taskVehicleRepository;
    @Mock private ExecutionRepository executionRepository;
    @Mock private TaskVehicleResetLogMapper resetLogMapper;

    @InjectMocks
    private VehicleTaskResetAppService service;

    private Task task;
    private final Instant releaseAt = Instant.now().minusSeconds(600);
    private final Instant startTime = Instant.now().minusSeconds(300);
    private Instant endTime = Instant.now().plusSeconds(3600);

    @BeforeEach
    void setUp() {
        task = Task.create(TaskId.of(TASK_ID), "任务", TaskType.NORMAL, ActivityId.of(ACTIVITY_ID));
        task.setPhase(TaskPhase.RELEASE);
        task.setState(TaskState.IN_PROGRESS);
        task.setReleaseTime(releaseAt);
        task.setStartTime(startTime);
        task.setEndTime(endTime);
        when(taskRepository.getById(TaskId.of(TASK_ID))).thenReturn(Optional.of(task));
        when(taskVehicleRepository.createResetTarget(any())).thenReturn(NEW_VEHICLE_TASK_ID);
        when(executionRepository.findActiveByVehicleTaskId(any())).thenReturn(Optional.empty());
    }

    /** 构造指定状态的车辆任务（默认无需授权、冻结摘要存在）。 */
    private VehicleTask vehicleTask(VehicleTaskStatus status) {
        return VehicleTask.reconstitute(
                VehicleTaskId.of(VEHICLE_TASK_ID), TASK_ID, VIN,
                TaskRevision.initial(), SnapshotDigest.of("digest-v1"),
                status, DownloadReadyState.NOT_STARTED, ConsentState.NOT_REQUIRED,
                releaseAt, startTime, endTime, null, null, null, null, 0, null,
                false, null, null, null, null, null, null, 3L);
    }

    private void stubVehicleTask(VehicleTask vt) {
        when(vehicleTaskRepository.getById(VehicleTaskId.of(VEHICLE_TASK_ID))).thenReturn(Optional.of(vt));
        when(vehicleTaskRepository.getByIdForUpdate(VehicleTaskId.of(VEHICLE_TASK_ID))).thenReturn(Optional.of(vt));
        when(vehicleTaskRepository.getByTaskIdAndVin(TaskId.of(TASK_ID), VIN)).thenReturn(Optional.of(vt));
    }

    private VehicleTaskResetCmd cmd(boolean force) {
        return VehicleTaskResetCmd.builder()
                .vehicleTaskId(VEHICLE_TASK_ID)
                .reason("测试环境卡在执行中，需重新下发")
                .force(force)
                .operator("hwyz_leo")
                .build();
    }

    @Test
    @DisplayName("待下载状态重置成功：旧任务 SUPERSEDED 并指向新任务，新建初始化车辆任务")
    void reset_downloadPending_supersedesAndCreatesNew() {
        VehicleTask vt = vehicleTask(VehicleTaskStatus.DOWNLOAD_PENDING);
        stubVehicleTask(vt);

        VehicleTaskResetResult result = service.reset(cmd(false));

        assertEquals(NEW_VEHICLE_TASK_ID, result.getNewVehicleTaskId());
        assertEquals(VehicleTaskStatus.SUPERSEDED.getValue(), result.getOldStatus());
        assertFalse(result.isForcedExecutionCanceled());
        assertEquals(VehicleTaskStatus.SUPERSEDED, vt.getStatus());
        assertEquals(NEW_VEHICLE_TASK_ID, vt.getSupersededBy().getValue());
        assertEquals("ABANDON", vt.getLocalDisposition());
        assertEquals("KEEP", vt.getPackageCacheAction());

        ArgumentCaptor<VehicleTaskResetSeed> seed = ArgumentCaptor.forClass(VehicleTaskResetSeed.class);
        verify(taskVehicleRepository).createResetTarget(seed.capture());
        VehicleTaskResetSeed s = seed.getValue();
        assertEquals(TASK_ID, s.taskId());
        assertEquals(VIN, s.vin());
        assertEquals(VehicleTaskStatus.DOWNLOAD_PENDING, s.status());
        assertEquals(ConsentState.NOT_REQUIRED, s.consentState());
        assertEquals(1L, s.taskRevision());
        assertEquals("digest-v1", s.snapshotDigest());
        assertEquals(releaseAt, s.releaseAt());
        assertEquals(startTime, s.startTime());
        assertEquals(endTime, s.endTime());
        assertEquals("hwyz_leo", s.operator());
    }

    @Test
    @DisplayName("需授权任务重置后回到待授权且不继承授权")
    void reset_consentRequired_goesToConsentPending() {
        VehicleTask vt = VehicleTask.reconstitute(
                VehicleTaskId.of(VEHICLE_TASK_ID), TASK_ID, VIN,
                TaskRevision.initial(), SnapshotDigest.of("digest-v1"),
                VehicleTaskStatus.EXECUTING, DownloadReadyState.VERIFIED, ConsentState.GRANTED,
                releaseAt, startTime, endTime, null, null, null, null, 2, null,
                true, 55L, "V1", "hash", "scope-digest", 77L, Instant.now(), 4L);
        stubVehicleTask(vt);

        service.reset(cmd(false));

        ArgumentCaptor<VehicleTaskResetSeed> seed = ArgumentCaptor.forClass(VehicleTaskResetSeed.class);
        verify(taskVehicleRepository).createResetTarget(seed.capture());
        assertEquals(VehicleTaskStatus.CONSENT_PENDING, seed.getValue().status());
        assertEquals(ConsentState.PENDING, seed.getValue().consentState());
        assertEquals(55L, seed.getValue().consentArticleId());
    }

    @Test
    @DisplayName("SUCCEEDED 拒绝重置（403002）")
    void reset_succeeded_rejected() {
        stubVehicleTask(vehicleTask(VehicleTaskStatus.SUCCEEDED));
        VehicleTaskResetException ex = assertThrows(VehicleTaskResetException.class, () -> service.reset(cmd(false)));
        assertEquals(VehicleTaskResetException.ERROR_CODE_SUCCEEDED_NOT_RESETTABLE, ex.getCode());
        verify(taskVehicleRepository, never()).createResetTarget(any());
    }

    @Test
    @DisplayName("历史 SUPERSEDED 行拒绝重置（403001）")
    void reset_alreadySuperseded_rejected() {
        VehicleTask vt = vehicleTask(VehicleTaskStatus.SUPERSEDED);
        stubVehicleTask(vt);
        VehicleTaskResetException ex = assertThrows(VehicleTaskResetException.class, () -> service.reset(cmd(false)));
        assertEquals(VehicleTaskResetException.ERROR_CODE_NOT_CURRENT, ex.getCode());
    }

    @Test
    @DisplayName("非当前行（存在其他当前行）拒绝重置（403001）")
    void reset_notCurrentRow_rejected() {
        VehicleTask vt = vehicleTask(VehicleTaskStatus.DOWNLOAD_PENDING);
        stubVehicleTask(vt);
        VehicleTask other = VehicleTask.reconstitute(
                VehicleTaskId.of(9999L), TASK_ID, VIN, TaskRevision.initial(), SnapshotDigest.of("d"),
                VehicleTaskStatus.DOWNLOAD_PENDING, DownloadReadyState.NOT_STARTED, ConsentState.NOT_REQUIRED,
                releaseAt, startTime, endTime, null, null, null, null, 0, null,
                false, null, null, null, null, null, null, 1L);
        when(vehicleTaskRepository.getByTaskIdAndVin(TaskId.of(TASK_ID), VIN)).thenReturn(Optional.of(other));

        VehicleTaskResetException ex = assertThrows(VehicleTaskResetException.class, () -> service.reset(cmd(false)));
        assertEquals(VehicleTaskResetException.ERROR_CODE_NOT_CURRENT, ex.getCode());
    }

    @Test
    @DisplayName("存在活动执行且未指定 force 拒绝重置（403003）")
    void reset_activeExecutionWithoutForce_rejected() {
        stubVehicleTask(vehicleTask(VehicleTaskStatus.EXECUTING));
        when(executionRepository.findActiveByVehicleTaskId(VehicleTaskId.of(VEHICLE_TASK_ID)))
                .thenReturn(Optional.of(execution()));

        VehicleTaskResetException ex = assertThrows(VehicleTaskResetException.class, () -> service.reset(cmd(false)));
        assertEquals(VehicleTaskResetException.ERROR_CODE_NEED_FORCE, ex.getCode());
        verify(executionRepository, never()).save(any());
        verify(taskVehicleRepository, never()).createResetTarget(any());
    }

    @Test
    @DisplayName("执行中重置（force=true）强制收口活动执行并返回执行信息")
    void reset_activeExecutionWithForce_cancelsExecution() {
        stubVehicleTask(vehicleTask(VehicleTaskStatus.EXECUTING));
        Execution execution = execution();
        when(executionRepository.findActiveByVehicleTaskId(VehicleTaskId.of(VEHICLE_TASK_ID)))
                .thenReturn(Optional.of(execution));

        VehicleTaskResetResult result = service.reset(cmd(true));

        assertTrue(result.isForcedExecutionCanceled());
        assertEquals(5001L, result.getOldExecutionId());
        assertEquals(ExecutionStatus.PERMITTED.getValue(), result.getOldExecutionStatus());
        assertEquals(ExecutionStatus.CANCELED, execution.getStatus());
        verify(executionRepository).save(execution);
        verify(taskVehicleRepository).createResetTarget(any());
    }

    @Test
    @DisplayName("父任务状态非 RELEASED/IN_PROGRESS 拒绝重置（403004）")
    void reset_taskStateBlocked_rejected() {
        stubVehicleTask(vehicleTask(VehicleTaskStatus.DOWNLOAD_PENDING));
        task.setState(TaskState.PAUSED);
        VehicleTaskResetException ex = assertThrows(VehicleTaskResetException.class, () -> service.reset(cmd(false)));
        assertEquals(VehicleTaskResetException.ERROR_CODE_TASK_STATE_BLOCKED, ex.getCode());
    }

    @Test
    @DisplayName("父任务已过 endTime 拒绝重置（403005）")
    void reset_windowExpired_rejected() {
        stubVehicleTask(vehicleTask(VehicleTaskStatus.DOWNLOAD_PENDING));
        task.setEndTime(Instant.now().minusSeconds(1));
        VehicleTaskResetException ex = assertThrows(VehicleTaskResetException.class, () -> service.reset(cmd(false)));
        assertEquals(VehicleTaskResetException.ERROR_CODE_WINDOW_EXPIRED, ex.getCode());
    }

    @Test
    @DisplayName("原因缺失或超长拒绝重置（403006）")
    void reset_reasonMissingOrTooLong_rejected() {
        stubVehicleTask(vehicleTask(VehicleTaskStatus.DOWNLOAD_PENDING));
        VehicleTaskResetCmd blank = VehicleTaskResetCmd.builder()
                .vehicleTaskId(VEHICLE_TASK_ID).reason("   ").operator("u").build();
        assertEquals(VehicleTaskResetException.ERROR_CODE_REASON_REQUIRED,
                assertThrows(VehicleTaskResetException.class, () -> service.reset(blank)).getCode());

        VehicleTaskResetCmd tooLong = VehicleTaskResetCmd.builder()
                .vehicleTaskId(VEHICLE_TASK_ID).reason("x".repeat(513)).operator("u").build();
        assertEquals(VehicleTaskResetException.ERROR_CODE_REASON_REQUIRED,
                assertThrows(VehicleTaskResetException.class, () -> service.reset(tooLong)).getCode());
    }

    @Test
    @DisplayName("rowVersion 冲突拒绝重置（403007）")
    void reset_rowVersionConflict_rejected() {
        stubVehicleTask(vehicleTask(VehicleTaskStatus.DOWNLOAD_PENDING));
        VehicleTaskResetCmd c = cmd(false);
        c.setRowVersion(999L);
        VehicleTaskResetException ex = assertThrows(VehicleTaskResetException.class, () -> service.reset(c));
        assertEquals(VehicleTaskResetException.ERROR_CODE_CONFLICT, ex.getCode());
    }

    @Test
    @DisplayName("审计记录字段完整且为追加写")
    void reset_writesAuditLog() {
        stubVehicleTask(vehicleTask(VehicleTaskStatus.ROLLED_BACK));
        service.reset(cmd(false));

        ArgumentCaptor<TaskVehicleResetLogPo> log = ArgumentCaptor.forClass(TaskVehicleResetLogPo.class);
        verify(resetLogMapper).insert(log.capture());
        TaskVehicleResetLogPo po = log.getValue();
        assertEquals(VEHICLE_TASK_ID, po.getOldVehicleTaskId());
        assertEquals(NEW_VEHICLE_TASK_ID, po.getNewVehicleTaskId());
        assertEquals(TASK_ID, po.getTaskId());
        assertEquals(ACTIVITY_ID, po.getActivityId());
        assertEquals(VIN, po.getVin());
        assertEquals(VehicleTaskStatus.SUPERSEDED.getValue(), po.getOldStatus());
        assertEquals(0, po.getForceFlag());
        assertEquals("测试环境卡在执行中，需重新下发", po.getReason());
        assertEquals("SUCCESS", po.getResult());
        assertEquals("hwyz_leo", po.getCreateBy());
        assertNull(po.getOldExecutionId());
        assertNotNull(po.getOldDownloadReady());
    }

    private Execution execution() {
        return Execution.permit(
                ExecutionId.of(5001L), VehicleTaskId.of(VEHICLE_TASK_ID), 1,
                TaskRevision.initial(), "PLAN_V1", SnapshotDigest.of("digest-v1"), "COND_V1",
                PermitToken.of("permit-token", Instant.now().plusSeconds(600)),
                Instant.now().plusSeconds(600));
    }
}
