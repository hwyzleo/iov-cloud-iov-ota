package net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleTaskStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VehicleTask 聚合根状态机测试（CR-012 §2.2）
 *
 * @author hwyz_leo
 */
@DisplayName("VehicleTask 聚合状态机")
class VehicleTaskTest {

    private VehicleTask vehicleTask;
    private final Instant now = Instant.now();
    private final Instant releaseAt = now.minusSeconds(60);
    private final Instant startTime = now.plusSeconds(60);
    private final Instant endTime = now.plusSeconds(3600);

    @BeforeEach
    void setUp() {
        vehicleTask = VehicleTask.create(
                VehicleTaskId.of(1L), 100L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("abc123"),
                releaseAt, startTime, endTime);
    }

    @Test
    @DisplayName("create 应设置 CREATED 状态")
    void create_shouldSetCreatedState() {
        assertEquals(VehicleTaskStatus.CREATED, vehicleTask.getStatus());
        assertEquals(ConsentState.NOT_REQUIRED, vehicleTask.getConsentState());
        assertEquals(DownloadReadyState.NOT_STARTED, vehicleTask.getDownloadReadyState());
        assertEquals(0, vehicleTask.getLastAttemptNo());
        assertFalse(vehicleTask.hasActiveExecution());
    }

    @Test
    @DisplayName("markVisible 在 releaseAt 后转为 VISIBLE")
    void markVisible_shouldTransitionToVisible() {
        vehicleTask.markVisible(now);
        assertEquals(VehicleTaskStatus.VISIBLE, vehicleTask.getStatus());
    }

    @Test
    @DisplayName("markVisible 在 releaseAt 前抛异常")
    void markVisible_beforeReleaseAt_throwsException() {
        assertThrows(VehicleTaskStateException.class,
                () -> vehicleTask.markVisible(releaseAt.minusSeconds(1)));
    }

    @Test
    @DisplayName("完整下载流程：VISIBLE -> CONSENT_PENDING -> DOWNLOAD_PENDING -> READY_TO_INSTALL")
    void downloadFlow() {
        vehicleTask.markVisible(now);
        vehicleTask.enterConsentPending();
        assertEquals(VehicleTaskStatus.CONSENT_PENDING, vehicleTask.getStatus());

        vehicleTask.grantConsent(true);
        assertEquals(VehicleTaskStatus.DOWNLOAD_PENDING, vehicleTask.getStatus());
        assertEquals(ConsentState.GRANTED, vehicleTask.getConsentState());

        vehicleTask.startDownload();
        assertEquals(DownloadReadyState.IN_PROGRESS, vehicleTask.getDownloadReadyState());

        vehicleTask.markDownloadReady();
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vehicleTask.getStatus());
        assertEquals(DownloadReadyState.VERIFIED, vehicleTask.getDownloadReadyState());
        assertTrue(vehicleTask.isInReadyState());
    }

    @Test
    @DisplayName("无需下载流程：授权后直接 READY_TO_INSTALL")
    void noDownloadFlow_grantConsentDirectlyReady() {
        vehicleTask.markVisible(now);
        vehicleTask.enterConsentPending();
        vehicleTask.grantConsent(false);
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vehicleTask.getStatus());
        assertEquals(DownloadReadyState.VERIFIED, vehicleTask.getDownloadReadyState());
    }

    @Test
    @DisplayName("attachExecution 转为 EXECUTING 并设置 attemptNo")
    void attachExecution_shouldTransitionToExecuting() {
        prepareReadyToInstall();
        vehicleTask.attachExecution(ExecutionId.of(1L), 1);
        assertEquals(VehicleTaskStatus.EXECUTING, vehicleTask.getStatus());
        assertEquals(1, vehicleTask.getLastAttemptNo());
        assertTrue(vehicleTask.hasActiveExecution());
    }

    @Test
    @DisplayName("重复 attachExecution 抛异常（活动执行唯一性）")
    void attachExecution_duplicate_throwsException() {
        prepareReadyToInstall();
        vehicleTask.attachExecution(ExecutionId.of(1L), 1);
        assertThrows(VehicleTaskStateException.class,
                () -> vehicleTask.attachExecution(ExecutionId.of(2L), 2));
    }

    @Test
    @DisplayName("执行成功 EXECUTING -> SUCCEEDED")
    void onExecutionSucceeded_shouldTransitionToSucceeded() {
        prepareReadyToInstall();
        vehicleTask.attachExecution(ExecutionId.of(1L), 1);
        vehicleTask.onExecutionSucceeded();
        assertEquals(VehicleTaskStatus.SUCCEEDED, vehicleTask.getStatus());
        assertFalse(vehicleTask.hasActiveExecution());
        assertTrue(vehicleTask.isTerminal());
    }

    @Test
    @DisplayName("执行失败 EXECUTING -> RETRY_PENDING -> READY_TO_INSTALL")
    void executionFailed_thenRetryReady() {
        prepareReadyToInstall();
        vehicleTask.attachExecution(ExecutionId.of(1L), 1);
        vehicleTask.onExecutionFailed();
        assertEquals(VehicleTaskStatus.RETRY_PENDING, vehicleTask.getStatus());

        vehicleTask.retryReady();
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vehicleTask.getStatus());
    }

    @Test
    @DisplayName("回滚完成可重试进入 RETRY_PENDING")
    void onExecutionRolledBack_canRetry() {
        prepareReadyToInstall();
        vehicleTask.attachExecution(ExecutionId.of(1L), 1);
        vehicleTask.onExecutionRolledBack(true);
        assertEquals(VehicleTaskStatus.RETRY_PENDING, vehicleTask.getStatus());
    }

    @Test
    @DisplayName("回滚完成不可重试进入 ROLLED_BACK 终态")
    void onExecutionRolledBack_cannotRetry() {
        prepareReadyToInstall();
        vehicleTask.attachExecution(ExecutionId.of(1L), 1);
        vehicleTask.onExecutionRolledBack(false);
        assertEquals(VehicleTaskStatus.ROLLED_BACK, vehicleTask.getStatus());
        assertTrue(vehicleTask.isTerminal());
    }

    @Test
    @DisplayName("pause 和 resume 恢复到暂停前状态")
    void pauseResume_shouldRestorePreviousState() {
        prepareReadyToInstall();
        vehicleTask.pause();
        assertEquals(VehicleTaskStatus.PAUSED, vehicleTask.getStatus());
        vehicleTask.resume();
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL, vehicleTask.getStatus());
    }

    @Test
    @DisplayName("cancel 清除活动执行并转为 CANCELED")
    void cancel_shouldTransitionToCanceled() {
        prepareReadyToInstall();
        vehicleTask.attachExecution(ExecutionId.of(1L), 1);
        vehicleTask.cancel();
        assertEquals(VehicleTaskStatus.CANCELED, vehicleTask.getStatus());
        assertFalse(vehicleTask.hasActiveExecution());
    }

    @Test
    @DisplayName("supersede 设置取代者并转为 SUPERSEDED")
    void supersede_shouldSetSupersededBy() {
        prepareReadyToInstall();
        vehicleTask.supersede(VehicleTaskId.of(2L));
        assertEquals(VehicleTaskStatus.SUPERSEDED, vehicleTask.getStatus());
        assertEquals(2L, vehicleTask.getSupersededBy().getValue());
    }

    @Test
    @DisplayName("upgradeRevision 升级版本号")
    void upgradeRevision_shouldIncrementRevision() {
        TaskRevision original = vehicleTask.getTaskRevision();
        TaskRevision next = original.next();
        vehicleTask.upgradeRevision(next, SnapshotDigest.of("newDigest"));
        assertEquals(next.getValue(), vehicleTask.getTaskRevision().getValue());
        assertEquals("newDigest", vehicleTask.getSnapshotDigest().getValue());
    }

    @Test
    @DisplayName("upgradeRevision 非递增版本号抛异常")
    void upgradeRevision_notIncrement_throwsException() {
        assertThrows(VehicleTaskStateException.class,
                () -> vehicleTask.upgradeRevision(TaskRevision.of(1L), SnapshotDigest.of("x")));
    }

    private void prepareReadyToInstall() {
        vehicleTask.markVisible(now);
        vehicleTask.enterConsentPending();
        vehicleTask.grantConsent(false);
    }
}
