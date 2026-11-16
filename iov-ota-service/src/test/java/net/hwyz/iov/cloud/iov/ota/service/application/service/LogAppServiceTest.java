package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.LogAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.LogObjectOperationException;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.LogObjectStorageGateway;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradeLog;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.UpgradeLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 日志闭环测试（IOV-OTA-DSN-CR-026 §7.3）：授权登记、对象核验、确定性失败与可恢复故障。
 *
 * @author hwyz_leo
 */
@DisplayName("LogAppService - 日志对象 OSS 闭环")
class LogAppServiceTest {

    private final VehicleTaskRepository vehicleTaskRepository = mock(VehicleTaskRepository.class);
    private final UpgradeLogRepository upgradeLogRepository = mock(UpgradeLogRepository.class);
    private final LogObjectStorageGateway gateway = mock(LogObjectStorageGateway.class);
    private final KafkaMessagingMetricsService metrics = mock(KafkaMessagingMetricsService.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

    private final String sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private LogAppService service;
    private VehicleTask vehicleTask;

    @BeforeEach
    void setUp() {
        service = new LogAppService(vehicleTaskRepository, upgradeLogRepository, gateway, metrics, txManager);
        Instant now = Instant.now();
        vehicleTask = VehicleTask.reconstitute(VehicleTaskId.of(9001L), 77L, "LSVAU2188N2ZG4G",
                TaskRevision.initial(), SnapshotDigest.of("digest"), VehicleTaskStatus.EXECUTING,
                DownloadReadyState.VERIFIED, ConsentState.NOT_REQUIRED, now, now, now.plusSeconds(3600),
                null, null, null, null, 0, null, false, null, null, null, null, null, null, 1L);
        when(vehicleTaskRepository.getById(VehicleTaskId.of(9001L))).thenReturn(Optional.of(vehicleTask));
        when(upgradeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private LogAuthCmd authCmd() {
        return LogAuthCmd.builder().vehicleTaskId(9001L).executionId(5001L).logRequestId("LOG-1")
                .fileName("fota.log").contentType("application/gzip").sizeBytes(1024L).sha256Hex(sha256)
                .privacyLevel("pii-masked").logScope("ccu-fota").desensitizeVersion("v1").build();
    }

    private UpgradeLog authorizedLog() {
        return UpgradeLog.builder().id(1L).taskId(77L).vin("LSVAU2188N2ZG4G").vehicleTaskId(9001L)
                .logRequestId("LOG-1").objectKey("logs/ota/log-1").logObjectId("obj-1")
                .logDigest(sha256).digestAlgorithm("sha-256").objectSizeBytes(1024L)
                .uploadState(UpgradeLog.STATE_AUTHORIZED).build();
    }

    @Test
    @DisplayName("授权：登记 AUTHORIZED 并返回预签名地址与强制头")
    void authorize_persistsAuthorized() {
        when(upgradeLogRepository.findByLogRequestId("LOG-1")).thenReturn(Optional.empty());
        when(gateway.authorize(any())).thenReturn(new LogObjectStorageGateway.Authorization(
                "obj-1", "logs/ota/log-1", "https://presigned", Map.of("x-cos-meta-sha256", sha256),
                Instant.now().plusSeconds(900), 536870912L));

        LogAuthResult result = service.authorizeLog(authCmd());

        assertEquals("logs/ota/log-1", result.getObjectKey());
        assertEquals("https://presigned", result.getUploadUrl());
        assertEquals(sha256, result.getHeaders().get("x-cos-meta-sha256"));
        ArgumentCaptor<UpgradeLog> captor = ArgumentCaptor.forClass(UpgradeLog.class);
        verify(upgradeLogRepository).save(captor.capture());
        assertEquals(UpgradeLog.STATE_AUTHORIZED, captor.getValue().getUploadState());
        assertEquals("obj-1", captor.getValue().getLogObjectId());
    }

    @Test
    @DisplayName("授权：缺失/非法摘要 fail-closed（403013），不调用存储")
    void authorize_invalidDigest_rejected() {
        LogAuthCmd cmd = authCmd();
        cmd.setSha256Hex("ABC");

        LogObjectOperationException exception = assertThrows(LogObjectOperationException.class,
                () -> service.authorizeLog(cmd));

        assertEquals(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT, exception.getCode());
        verify(gateway, never()).authorize(any());
    }

    @Test
    @DisplayName("结果：HEAD 核验通过才 UPLOADED")
    void result_verified_markedUploaded() {
        when(upgradeLogRepository.findByLogRequestId("LOG-1")).thenReturn(Optional.of(authorizedLog()));
        when(gateway.complete("obj-1")).thenReturn(new LogObjectStorageGateway.Fact("obj-1", "logs/ota/log-1",
                "UPLOADED", 1024L, 1024L, sha256, sha256, "etag-1", null,
                Instant.now(), Instant.now(), Instant.now()));

        service.submitLogResult(LogResultCmd.builder().vehicleTaskId(9001L).logRequestId("LOG-1")
                .objectKey("logs/ota/log-1").logDigest(sha256).uploadResult("SUCCESS").build());

        ArgumentCaptor<UpgradeLog> captor = ArgumentCaptor.forClass(UpgradeLog.class);
        verify(upgradeLogRepository).save(captor.capture());
        assertEquals(UpgradeLog.STATE_UPLOADED, captor.getValue().getUploadState());
    }

    @Test
    @DisplayName("结果：对象键与授权不一致即确定性失败 403012，且不调用存储")
    void result_objectKeyMismatch_rejected() {
        UpgradeLog logEntity = authorizedLog();
        when(upgradeLogRepository.findByLogRequestId("LOG-1")).thenReturn(Optional.of(logEntity));

        LogObjectOperationException exception = assertThrows(LogObjectOperationException.class,
                () -> service.submitLogResult(LogResultCmd.builder().vehicleTaskId(9001L)
                        .logRequestId("LOG-1").objectKey("logs/ota/other").uploadResult("SUCCESS").build()));

        assertEquals(LogObjectOperationException.ERROR_CODE_OBJECT_MISMATCH, exception.getCode());
        assertFalse(exception.isRecoverable());
        assertEquals(UpgradeLog.STATE_FAILED, logEntity.getUploadState());
        verify(gateway, never()).complete(any());
    }

    @Test
    @DisplayName("结果：对象不存在 403011 并保留 FAILED")
    void result_objectNotFound_failed() {
        UpgradeLog logEntity = authorizedLog();
        when(upgradeLogRepository.findByLogRequestId("LOG-1")).thenReturn(Optional.of(logEntity));
        when(gateway.complete("obj-1")).thenThrow(new LogObjectOperationException(
                LogObjectOperationException.ERROR_CODE_OBJECT_NOT_FOUND, "对象不存在"));

        LogObjectOperationException exception = assertThrows(LogObjectOperationException.class,
                () -> service.submitLogResult(LogResultCmd.builder().vehicleTaskId(9001L)
                        .logRequestId("LOG-1").objectKey("logs/ota/log-1").uploadResult("SUCCESS").build()));

        assertEquals(LogObjectOperationException.ERROR_CODE_OBJECT_NOT_FOUND, exception.getCode());
        assertEquals(UpgradeLog.STATE_FAILED, logEntity.getUploadState());
        assertEquals("OBJECT_NOT_FOUND", logEntity.getFailureReason());
    }

    @Test
    @DisplayName("结果：存储暂不可用 403016 可恢复，不置 UPLOADED/FAILED")
    void result_storageUnavailable_recoverable() {
        UpgradeLog logEntity = authorizedLog();
        when(upgradeLogRepository.findByLogRequestId("LOG-1")).thenReturn(Optional.of(logEntity));
        when(gateway.complete("obj-1")).thenThrow(new LogObjectOperationException(
                LogObjectOperationException.ERROR_CODE_STORAGE_UNAVAILABLE, "COS 超时", true));

        LogObjectOperationException exception = assertThrows(LogObjectOperationException.class,
                () -> service.submitLogResult(LogResultCmd.builder().vehicleTaskId(9001L)
                        .logRequestId("LOG-1").objectKey("logs/ota/log-1").uploadResult("SUCCESS").build()));

        assertTrue(exception.isRecoverable());
        assertEquals(UpgradeLog.STATE_AUTHORIZED, logEntity.getUploadState(), "可恢复故障不得改写状态");
        verify(upgradeLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("结果：已 UPLOADED 幂等返回，不重复核验")
    void result_alreadyUploaded_idempotent() {
        UpgradeLog uploaded = authorizedLog().markUploaded(Instant.now());
        when(upgradeLogRepository.findByLogRequestId("LOG-1")).thenReturn(Optional.of(uploaded));

        service.submitLogResult(LogResultCmd.builder().vehicleTaskId(9001L).logRequestId("LOG-1")
                .objectKey("logs/ota/log-1").uploadResult("SUCCESS").build());

        verify(gateway, never()).complete(any());
        verify(upgradeLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("结果：车端明确上传失败直接记 FAILED，不调用存储")
    void result_vehicleFailure_markedFailed() {
        UpgradeLog logEntity = authorizedLog();
        when(upgradeLogRepository.findByLogRequestId("LOG-1")).thenReturn(Optional.of(logEntity));

        service.submitLogResult(LogResultCmd.builder().vehicleTaskId(9001L).logRequestId("LOG-1")
                .objectKey("logs/ota/log-1").uploadResult("FAIL").errorCode("UPLOAD_TIMEOUT").build());

        assertEquals(UpgradeLog.STATE_FAILED, logEntity.getUploadState());
        assertEquals("UPLOAD_TIMEOUT", logEntity.getFailureReason());
        verify(gateway, never()).complete(any());
    }

    @Test
    @DisplayName("结果：未知登记 403013 确定性失败")
    void result_unknownRegistration_conflict() {
        when(upgradeLogRepository.findByLogRequestId(any())).thenReturn(Optional.empty());
        when(upgradeLogRepository.findByObjectKey(any())).thenReturn(Optional.empty());

        LogObjectOperationException exception = assertThrows(LogObjectOperationException.class,
                () -> service.submitLogResult(LogResultCmd.builder().vehicleTaskId(9001L)
                        .logRequestId("LOG-X").objectKey("logs/ota/x").uploadResult("SUCCESS").build()));

        assertEquals(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT, exception.getCode());
    }
}
