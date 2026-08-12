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
        when(executionRepository.getById(ExecutionId.of(1L))).thenReturn(Optional.of(execution));
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
}
