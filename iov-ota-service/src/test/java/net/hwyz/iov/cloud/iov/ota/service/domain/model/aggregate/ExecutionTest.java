package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ExecutionStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.PermitToken;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SequenceWatermark;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Execution 聚合根测试（CR-012 §2.3、§5.6、§5.7）
 *
 * @author hwyz_leo
 */
@DisplayName("Execution 聚合状态机与事件水位")
class ExecutionTest {

    private Execution execution;
    private final Instant now = Instant.now();
    private final Instant validUntil = now.plusSeconds(3600);

    @BeforeEach
    void setUp() {
        execution = Execution.permit(
                ExecutionId.of(1L), VehicleTaskId.of(10L), 1,
                TaskRevision.initial(), "PLAN_V1",
                SnapshotDigest.of("manifestDigest"), "COND_V1",
                PermitToken.of("permit-token-001", validUntil), validUntil);
    }

    @Test
    @DisplayName("permit 应设置 PERMITTED 状态和冻结策略")
    void permit_shouldSetPermittedStatus() {
        assertEquals(ExecutionStatus.PERMITTED, execution.getStatus());
        assertEquals(1, execution.getAttemptNo());
        assertEquals("PLAN_V1", execution.getInstallPlanVersion());
        assertEquals("permit-token-001", execution.getPermitToken().getToken());
        assertEquals(0L, execution.getSequenceWatermark().getAcceptedSequenceNo());
        assertTrue(execution.isActive());
    }

    @Test
    @DisplayName("startInstall 在许可有效期内转为 INSTALLING")
    void startInstall_withinValidUntil_shouldTransition() {
        execution.startInstall(now);
        assertEquals(ExecutionStatus.INSTALLING, execution.getStatus());
    }

    @Test
    @DisplayName("startInstall 许可过期抛异常（validUntil 仅限制进入 INSTALL_STARTED）")
    void startInstall_afterValidUntil_throwsException() {
        Execution expired = Execution.permit(
                ExecutionId.of(2L), VehicleTaskId.of(10L), 2,
                TaskRevision.initial(), "PLAN_V1", SnapshotDigest.of("d"), "C",
                PermitToken.of("tok", now.minusSeconds(1)), now.minusSeconds(1));
        assertThrows(ExecutionStateException.class, () -> expired.startInstall(now));
    }

    @Test
    @DisplayName("INSTALLING -> SUCCEEDED")
    void succeed_shouldTransitionToSucceeded() {
        execution.startInstall(now);
        execution.succeed();
        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
        assertTrue(execution.isTerminal());
    }

    @Test
    @DisplayName("INSTALLING -> PAUSED -> INSTALLING")
    void pauseResume_shouldRestoreInstalling() {
        execution.startInstall(now);
        execution.pause();
        assertEquals(ExecutionStatus.PAUSED, execution.getStatus());
        execution.resume();
        assertEquals(ExecutionStatus.INSTALLING, execution.getStatus());
    }

    @Test
    @DisplayName("INSTALLING -> ROLLING_BACK -> ROLLED_BACK")
    void rollbackFlow() {
        execution.startInstall(now);
        execution.startRollback();
        assertEquals(ExecutionStatus.ROLLING_BACK, execution.getStatus());
        execution.rolledBack();
        assertEquals(ExecutionStatus.ROLLED_BACK, execution.getStatus());
        assertTrue(execution.isTerminal());
    }

    @Test
    @DisplayName("INSTALLING -> FAILED")
    void fail_shouldTransitionToFailed() {
        execution.startInstall(now);
        execution.fail();
        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertTrue(execution.isTerminal());
    }

    @Test
    @DisplayName("PERMITTED -> CANCELED")
    void cancel_shouldTransitionToCanceled() {
        execution.cancel();
        assertEquals(ExecutionStatus.CANCELED, execution.getStatus());
    }

    @Test
    @DisplayName("INSTALLING -> TIMED_OUT")
    void timeout_shouldTransitionToTimedOut() {
        execution.startInstall(now);
        execution.timeout();
        assertEquals(ExecutionStatus.TIMED_OUT, execution.getStatus());
    }

    @Test
    @DisplayName("接收连续事件推进水位")
    void receiveEvent_sequential_advancesWatermark() {
        assertEquals(SequenceWatermark.Disposition.ACCEPTED, execution.receiveEvent(1));
        assertEquals(1L, execution.getSequenceWatermark().getAcceptedSequenceNo());
        assertEquals(SequenceWatermark.Disposition.ACCEPTED, execution.receiveEvent(2));
        assertEquals(2L, execution.getSequenceWatermark().getAcceptedSequenceNo());
    }

    @Test
    @DisplayName("接收乱序事件标记 BUFFERED 不推进水位")
    void receiveEvent_outOfOrder_buffered() {
        assertEquals(SequenceWatermark.Disposition.BUFFERED, execution.receiveEvent(3));
        assertEquals(0L, execution.getSequenceWatermark().getAcceptedSequenceNo());
        // 1 到达后推进并吸收
        assertEquals(SequenceWatermark.Disposition.ACCEPTED, execution.receiveEvent(1));
        assertEquals(1L, execution.getSequenceWatermark().getAcceptedSequenceNo());
        // 2 到达吸收
        execution.receiveEvent(2);
        assertEquals(3L, execution.getSequenceWatermark().getAcceptedSequenceNo());
    }

    @Test
    @DisplayName("接收重复事件标记 DUPLICATE")
    void receiveEvent_duplicate() {
        execution.receiveEvent(1);
        assertEquals(SequenceWatermark.Disposition.DUPLICATE, execution.receiveEvent(1));
    }

    @Test
    @DisplayName("finalize 水位未达最终序号抛异常")
    void finalize_watermarkNotReached_throwsException() {
        execution.defineFinalSequenceNo(5);
        execution.receiveEvent(1);
        assertThrows(ExecutionStateException.class, () -> execution.finalize(ExecutionStatus.SUCCEEDED));
    }

    @Test
    @DisplayName("finalize 水位达到最终序号后收口")
    void finalize_watermarkReached_shouldClose() {
        execution.defineFinalSequenceNo(3);
        execution.receiveEvent(1);
        execution.receiveEvent(2);
        execution.receiveEvent(3);
        assertTrue(execution.isWatermarkReached());
        execution.finalize(ExecutionStatus.SUCCEEDED);
        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
    }
}
