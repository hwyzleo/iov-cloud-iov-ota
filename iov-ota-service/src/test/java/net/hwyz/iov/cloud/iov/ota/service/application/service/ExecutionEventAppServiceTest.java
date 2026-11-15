package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionEventCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionEventResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.Execution;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.ExecutionRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlAckMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionControlMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ExecutionEventMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionControlPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ExecutionEventPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ExecutionEventAppService 测试（CR-012 §5.6、US-080）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExecutionEventAppService 事件水位处理")
class ExecutionEventAppServiceTest {

    @Mock private ExecutionRepository executionRepository;
    @Mock private ExecutionEventMapper executionEventMapper;
    @Mock private ExecutionControlMapper executionControlMapper;
    @Mock private ExecutionControlAckMapper executionControlAckMapper;

    @InjectMocks
    private ExecutionEventAppService service;

    private Execution execution;

    @BeforeEach
    void setUp() {
        Instant validUntil = Instant.now().plusSeconds(3600);
        execution = Execution.permit(
                ExecutionId.of(1L), VehicleTaskId.of(10L), 1,
                TaskRevision.initial(), "PLAN_V1",
                SnapshotDigest.of("digest"), "COND_V1",
                PermitToken.of("token", validUntil), validUntil);
        when(executionRepository.getByIdForUpdate(ExecutionId.of(1L))).thenReturn(Optional.of(execution));
        when(executionEventMapper.selectReceivedSequenceNos(any())).thenReturn(java.util.List.of());
        when(executionEventMapper.selectByEventId(any())).thenReturn(null);
    }

    @Test
    @DisplayName("连续事件 ACCEPTED 并推进水位")
    void receiveEvent_sequential_accepted() {
        ExecutionEventCmd cmd1 = ExecutionEventCmd.builder()
                .executionId(1L).eventId("evt-1").sequenceNo(1L).eventType("INSTALL_STARTED").build();

        ExecutionEventResult result1 = service.receiveEvent(cmd1);

        assertEquals("ACCEPTED", result1.getDisposition());
        assertEquals(1L, result1.getAcceptedSequenceNo());
        assertTrue(result1.getMissingSequenceRanges().isEmpty());
        verify(executionEventMapper).insert(any(ExecutionEventPo.class));
        verify(executionRepository).save(any());
    }

    @Test
    @DisplayName("乱序事件 BUFFERED 不推进水位")
    void receiveEvent_outOfOrder_buffered() {
        ExecutionEventCmd cmd = ExecutionEventCmd.builder()
                .executionId(1L).eventId("evt-3").sequenceNo(3L).eventType("PROGRESS").build();

        ExecutionEventResult result = service.receiveEvent(cmd);

        assertEquals("BUFFERED", result.getDisposition());
        assertEquals(0L, result.getAcceptedSequenceNo());
        assertFalse(result.getMissingSequenceRanges().isEmpty());
    }

    @Test
    @DisplayName("重复 eventId 幂等返回原处置")
    void receiveEvent_duplicateEventId_idempotent() {
        ExecutionEventPo existingPo = ExecutionEventPo.builder()
                .eventId("evt-1").sequenceNo(1L).disposition("ACCEPTED").build();
        when(executionEventMapper.selectByEventId("evt-1")).thenReturn(existingPo);

        ExecutionEventCmd cmd = ExecutionEventCmd.builder()
                .executionId(1L).eventId("evt-1").sequenceNo(1L).build();

        ExecutionEventResult result = service.receiveEvent(cmd);

        assertEquals("ACCEPTED", result.getDisposition());
        verify(executionEventMapper, never()).insert(any());
    }

    @Test
    @DisplayName("事件响应携带最新有效控制")
    void receiveEvent_returnsLatestControl() {
        ExecutionControlPo control = ExecutionControlPo.builder()
                .controlId("ctrl-1").executionId(1L).controlRevision(3).action("CONTINUE").build();
        when(executionControlMapper.selectLatestByExecutionId(1L)).thenReturn(control);

        ExecutionEventCmd cmd = ExecutionEventCmd.builder()
                .executionId(1L).eventId("evt-1").sequenceNo(1L).build();

        ExecutionEventResult result = service.receiveEvent(cmd);

        assertEquals(3, result.getLatestControlRevision());
        assertEquals("CONTINUE", result.getLatestControlAction());
    }

    @Test
    @DisplayName("CR-025 终态执行拒绝迟到事件：不推进水位、不落事件（LATE_EVENT）")
    void receiveEvent_terminalExecution_returnsLateEvent() {
        execution.cancel();
        when(executionRepository.getByIdForUpdate(ExecutionId.of(1L))).thenReturn(Optional.of(execution));
        when(executionEventMapper.selectReceivedSequenceNos(any())).thenReturn(java.util.List.of());

        ExecutionEventResult result = service.receiveEvent(ExecutionEventCmd.builder()
                .executionId(1L).eventId("evt-late").sequenceNo(9L).build());

        assertEquals("LATE_EVENT", result.getDisposition());
        assertEquals(execution.getSequenceWatermark().getAcceptedSequenceNo(), result.getAcceptedSequenceNo());
        verify(executionEventMapper, never()).insert(any());
        verify(executionRepository, never()).save(any());
    }

    @Test
    @DisplayName("重载后从事件表重建乱序缓冲：到达空缺序号即一次吸收并回非空缺失范围")
    void receiveEvent_restoresBufferedFromEventTable_andAbsorbs() {
        // 实车 2026-11-15：buffered 只在内存，重载后 missing ranges 恒为空、水位无法吸收
        when(executionEventMapper.selectReceivedSequenceNos(1L))
                .thenReturn(java.util.List.of(2L, 3L));

        ExecutionEventResult before = service.receiveEvent(ExecutionEventCmd.builder()
                .executionId(1L).eventId("evt-9").sequenceNo(9L).build());
        assertEquals("BUFFERED", before.getDisposition());
        assertEquals(0L, before.getAcceptedSequenceNo());
        assertEquals(2, before.getMissingSequenceRanges().size());
        assertEquals(1L, before.getMissingSequenceRanges().get(0)[0]);
        assertEquals(1L, before.getMissingSequenceRanges().get(0)[1]);
        assertEquals(4L, before.getMissingSequenceRanges().get(1)[0]);
        assertEquals(8L, before.getMissingSequenceRanges().get(1)[1]);
    }

    @Test
    @DisplayName("空缺序号到达后循环吸收已暂存事件，并把吸收行置为 ACCEPTED")
    void receiveEvent_gapFilled_absorbsBufferedAndMarksAccepted() {
        when(executionEventMapper.selectReceivedSequenceNos(1L))
                .thenReturn(java.util.List.of(2L, 3L));

        ExecutionEventResult result = service.receiveEvent(ExecutionEventCmd.builder()
                .executionId(1L).eventId("evt-1").sequenceNo(1L).build());

        assertEquals("ACCEPTED", result.getDisposition());
        assertEquals(3L, result.getAcceptedSequenceNo());
        assertTrue(result.getMissingSequenceRanges().isEmpty());
        verify(executionRepository).save(execution);
        verify(executionEventMapper).markBufferedAbsorbed(1L, 3L);
    }

    @Test
    @DisplayName("幂等重放：序号已在水位内按 ACCEPTED 应答（车端仅 ACCEPTED 才 ACK，避免补发死循环）")
    void receiveEvent_replayedEventWithinWatermark_respondsAccepted() {
        when(executionEventMapper.selectReceivedSequenceNos(1L))
                .thenReturn(java.util.List.of(1L));
        ExecutionEventPo existingPo = ExecutionEventPo.builder()
                .eventId("evt-1").executionId(1L).sequenceNo(1L).disposition("BUFFERED").build();
        when(executionEventMapper.selectByEventId("evt-1")).thenReturn(existingPo);

        ExecutionEventResult result = service.receiveEvent(ExecutionEventCmd.builder()
                .executionId(1L).eventId("evt-1").sequenceNo(1L).build());

        assertEquals("ACCEPTED", result.getDisposition());
        assertEquals(1L, result.getAcceptedSequenceNo());
        verify(executionEventMapper, never()).insert(any());
        verify(executionRepository).save(execution);
    }

    @Test
    @DisplayName("水位被写回小值后自愈：以事件表连续前缀为准，重投即刻推进到真实水位")
    void receiveEvent_regressedWatermark_healedFromEventFacts() {
        // 实车现场：DB 列 accepted_sequence_no=1，但事件表已有 1、2、3（旧实现整行覆盖写倒退）
        Instant validUntil = Instant.now().plusSeconds(3600);
        Execution regressed = Execution.reconstitute(
                ExecutionId.of(1L), VehicleTaskId.of(10L), 1,
                net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus.INSTALLING,
                TaskRevision.initial(), "PLAN_V1", SnapshotDigest.of("digest"), "COND_V1",
                PermitToken.of("token", validUntil), validUntil,
                1L, 0L, null, null, null);
        when(executionRepository.getByIdForUpdate(ExecutionId.of(1L))).thenReturn(Optional.of(regressed));
        when(executionEventMapper.selectReceivedSequenceNos(1L))
                .thenReturn(java.util.List.of(1L, 2L, 3L, 5L));
        when(executionEventMapper.selectByEventId("evt-3")).thenReturn(ExecutionEventPo.builder()
                .eventId("evt-3").executionId(1L).sequenceNo(3L).disposition("BUFFERED").build());

        ExecutionEventResult result = service.receiveEvent(ExecutionEventCmd.builder()
                .executionId(1L).eventId("evt-3").sequenceNo(3L).build());

        assertEquals("ACCEPTED", result.getDisposition());
        assertEquals(3L, result.getAcceptedSequenceNo());
        // 缺口 4 仍在 buffered 中回显
        assertEquals(1, result.getMissingSequenceRanges().size());
        assertEquals(4L, result.getMissingSequenceRanges().get(0)[0]);
        assertEquals(4L, result.getMissingSequenceRanges().get(0)[1]);
        verify(executionRepository).save(regressed);
    }
}
