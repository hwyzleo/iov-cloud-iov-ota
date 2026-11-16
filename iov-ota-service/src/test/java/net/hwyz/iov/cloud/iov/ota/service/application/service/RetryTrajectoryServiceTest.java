package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.TaskVehicleRetryLog;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.TaskVehicleRetryLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 重试/续传轨迹写入测试（IOV-OTA-DSN-CR-026 §7.4）。
 *
 * @author hwyz_leo
 */
@DisplayName("RetryTrajectoryService - 业务消息身份幂等轨迹")
class RetryTrajectoryServiceTest {

    private final TaskVehicleRetryLogRepository repository = mock(TaskVehicleRetryLogRepository.class);
    private final KafkaMessagingMetricsService metrics = mock(KafkaMessagingMetricsService.class);
    private final RetryTrajectoryService service = new RetryTrajectoryService(repository, metrics);

    private VehicleTask vehicleTask() {
        Instant now = Instant.now();
        return VehicleTask.reconstitute(VehicleTaskId.of(9001L), 77L, "LSVAU2188N2ZG4G", TaskRevision.initial(),
                SnapshotDigest.of("digest"), VehicleTaskStatus.CREATED, DownloadReadyState.NOT_STARTED,
                ConsentState.NOT_REQUIRED, now, now, now.plusSeconds(3600), null, null, null, null, 0,
                null, false, null, null, null, null, null, null, 1L);
    }

    @Test
    @DisplayName("同一业务事实技术重投：retryEventId 稳定，不追加新行")
    void recordExecutionCreated_sameFactSameEventId() {
        when(repository.appendIdempotent(any())).thenAnswer(inv -> inv.getArgument(0));
        Instant received = Instant.ofEpochMilli(1_800_000_000_000L);

        service.recordExecutionCreated(vehicleTask(), 1, received);
        service.recordExecutionCreated(vehicleTask(), 1, received.plusSeconds(5));

        ArgumentCaptor<TaskVehicleRetryLog> captor = ArgumentCaptor.forClass(TaskVehicleRetryLog.class);
        verify(repository, times(2)).appendIdempotent(captor.capture());
        assertEquals(captor.getAllValues().get(0).getRetryEventId(), captor.getAllValues().get(1).getRetryEventId(),
                "同一业务事实（执行创建）技术重投必须复用同一 retryEventId");
        assertEquals(RetryTrajectoryService.ACTION_EXECUTION_CREATED, captor.getValue().getAction());
        assertEquals(9001L, captor.getValue().getVehicleTaskId());
    }

    @Test
    @DisplayName("第二次安装尝试记为安装重试 RETRY")
    void recordExecutionCreated_secondAttemptIsRetry() {
        when(repository.appendIdempotent(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordExecutionCreated(vehicleTask(), 2, Instant.now());

        ArgumentCaptor<TaskVehicleRetryLog> captor = ArgumentCaptor.forClass(TaskVehicleRetryLog.class);
        verify(repository).appendIdempotent(captor.capture());
        assertEquals(RetryTrajectoryService.ACTION_RETRY, captor.getValue().getAction());
        assertEquals(RetryTrajectoryService.STAGE_INSTALL, captor.getValue().getStage());
    }

    @Test
    @DisplayName("下载续传/重置偏移：包与动作不同则事件ID不同，未知车端时间保持 null")
    void recordDownloadOperation_distinguishesActionAndPackage() {
        when(repository.appendIdempotent(any())).thenAnswer(inv -> inv.getArgument(0));
        VehicleTask vt = vehicleTask();

        service.recordDownloadOperation(vt, "PKG-A", 1, RetryTrajectoryService.ACTION_RESUME, 1024L,
                RetryTrajectoryService.RESULT_SUCCESS, "weak-network", null, null, Instant.now());
        service.recordDownloadOperation(vt, "PKG-B", 1, RetryTrajectoryService.ACTION_RESUME, 1024L,
                RetryTrajectoryService.RESULT_SUCCESS, "weak-network", null, null, Instant.now());

        ArgumentCaptor<TaskVehicleRetryLog> captor = ArgumentCaptor.forClass(TaskVehicleRetryLog.class);
        verify(repository, times(2)).appendIdempotent(captor.capture());
        assertNotEquals(captor.getAllValues().get(0).getRetryEventId(), captor.getAllValues().get(1).getRetryEventId());
        assertNull(captor.getAllValues().get(0).getSourceOccurredAt(), "车端发生时间未知不得回填");
    }

    @Test
    @DisplayName("eventId 对同一业务身份恒定、对尝试序号敏感")
    void eventId_isDeterministic() {
        String first = RetryTrajectoryService.eventId(1L, "PKG", "DOWNLOAD", "RESUME", 1, 10L, "t");
        String same = RetryTrajectoryService.eventId(1L, "PKG", "DOWNLOAD", "RESUME", 1, 10L, "t");
        String otherAttempt = RetryTrajectoryService.eventId(1L, "PKG", "DOWNLOAD", "RESUME", 2, 10L, "t");

        assertEquals(first, same);
        assertNotEquals(first, otherAttempt);
    }
}
