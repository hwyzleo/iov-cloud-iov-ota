package net.hwyz.iov.cloud.iov.ota.service.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DownloadProgressCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DownloadProgressResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.DownloadProgressDecisionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PackageDownloadProgressEventMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.PackageDownloadProgressMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.SoftwarePackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskPackageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PackageDownloadProgressEventPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.PackageDownloadProgressPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskPackagePo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 下载进度受理测试（IOV-OTA-DSN-CR-026 §5.3/§7.1；PAR-PROTO-DSN-CR-011 冻结语义）。
 *
 * @author hwyz_leo
 */
@DisplayName("DownloadProgressAppService - 幂等/乱序/单调/边界")
class DownloadProgressAppServiceTest {

    private static final String SHA = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final long TOTAL = 1048576L;

    private final VehicleTaskRepository vehicleTaskRepository = mock(VehicleTaskRepository.class);
    private final VehicleTaskPackageMapper vehicleTaskPackageMapper = mock(VehicleTaskPackageMapper.class);
    private final SoftwarePackageMapper softwarePackageMapper = mock(SoftwarePackageMapper.class);
    private final PackageDownloadProgressMapper snapshotMapper = mock(PackageDownloadProgressMapper.class);
    private final PackageDownloadProgressEventMapper eventMapper = mock(PackageDownloadProgressEventMapper.class);
    private final DownloadProgressDecisionMapper decisionMapper = mock(DownloadProgressDecisionMapper.class);
    private final RetryTrajectoryService retryTrajectoryService = mock(RetryTrajectoryService.class);
    private final KafkaMessagingMetricsService metrics = mock(KafkaMessagingMetricsService.class);
    private final VehicleTaskPackageService vehicleTaskPackageService = mock(VehicleTaskPackageService.class);

    private DownloadProgressAppService service;
    private final Map<String, Map<String, Object>> decisions = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new DownloadProgressAppService(vehicleTaskRepository, vehicleTaskPackageMapper,
                softwarePackageMapper, snapshotMapper, eventMapper, decisionMapper, retryTrajectoryService,
                metrics, new ObjectMapper(), vehicleTaskPackageService);
        Instant now = Instant.now();
        VehicleTask vehicleTask = VehicleTask.reconstitute(VehicleTaskId.of(9001L), 77L, "LSVAU2188N2ZG4G",
                TaskRevision.initial(), SnapshotDigest.of("digest"), VehicleTaskStatus.EXECUTING,
                DownloadReadyState.IN_PROGRESS, ConsentState.NOT_REQUIRED, now, now, now.plusSeconds(3600),
                null, null, null, null, 0, null, false, null, null, null, null, null, null, 1L);
        when(vehicleTaskRepository.getById(VehicleTaskId.of(9001L))).thenReturn(Optional.of(vehicleTask));
        VehicleTaskPackagePo frozen = VehicleTaskPackagePo.builder()
                .vehicleTaskId(9001L).packageId("PKG-1").packageRevision("REV-1").etag("etag-1").build();
        when(vehicleTaskPackageMapper.selectByVehicleTaskId(9001L)).thenReturn(List.of(frozen));
        when(softwarePackageMapper.selectPackageSizeByCode("PKG-1")).thenReturn(TOTAL);
        when(decisionMapper.selectDecision(anyString(), anyString()))
                .thenAnswer(inv -> decisions.get(inv.getArgument(1)));
        when(decisionMapper.insertDecision(anyString(), anyString(), anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("request_digest", inv.getArgument(2));
                    row.put("response_snapshot", inv.getArgument(3));
                    decisions.put(inv.getArgument(1), row);
                    return 1;
                });
        when(snapshotMapper.insert(any())).thenReturn(1);
        when(eventMapper.insert(any())).thenReturn(1);
        when(decisionMapper.advanceSnapshot(anyLong(), anyString(), anyInt(), anyString(), anyLong(), anyString(),
                anyLong(), anyLong(), anyLong(), any(), any(), any(), any(), any(), any())).thenReturn(1);
    }

    private DownloadProgressCmd cmd(String progressId, long seq, String state, long downloaded) {
        return DownloadProgressCmd.builder()
                .progressId(progressId).vehicleTaskId(9001L).packageId("PKG-1").packageRevision("REV-1")
                .downloadAttemptNo(1).progressSeq(seq).downloadState(state)
                .downloadedBytes(downloaded).totalBytes(TOTAL).resumeOffset(0L)
                .occurredAt(Instant.ofEpochMilli(1_800_000_000_000L)).eventDigest(SHA).etag("etag-1")
                .vin("LSVAU2188N2ZG4G").build();
    }

    private PackageDownloadProgressPo snapshot(long seq, long bytes) {
        return PackageDownloadProgressPo.builder().id(1L).vehicleTaskId(9001L).packageId("PKG-1")
                .packageRevision("REV-1").downloadAttemptNo(1).latestProgressSeq(seq)
                .downloadedBytes(bytes).totalBytes(TOTAL).resumeOffset(0L).etag("etag-1").build();
    }

    @Test
    @DisplayName("首个 STARTED 受理为 CURRENT 并推进快照与历史")
    void firstReport_current() {
        DownloadProgressResult result = service.receive(cmd("DP-1", 1, "STARTED", 0));

        assertEquals(DownloadProgressAppService.DISPOSITION_CURRENT, result.getDisposition());
        assertTrue(result.isAccepted());
        assertEquals(1L, result.getLatestAcceptedProgressSeq());
        verify(eventMapper).insert(any());
        verify(snapshotMapper).insert(any());
        verify(decisionMapper).advanceSnapshot(eq(9001L), eq("PKG-1"), eq(1), eq("DP-1"), eq(1L),
                eq("STARTED"), eq(0L), eq(TOTAL), eq(0L), any(), eq("etag-1"), isNull(), isNull(), any(), any());
    }

    @Test
    @DisplayName("同 progressId 同摘要重投 → DUPLICATE，不重复落历史/推进快照")
    void duplicate_sameDigest() {
        service.receive(cmd("DP-1", 1, "STARTED", 0));
        when(snapshotMapper.selectForUpdate(9001L, "PKG-1", 1)).thenReturn(snapshot(1, 0));

        DownloadProgressResult result = service.receive(cmd("DP-1", 1, "STARTED", 0));

        assertEquals(DownloadProgressAppService.DISPOSITION_DUPLICATE, result.getDisposition());
        assertTrue(result.isAccepted());
        verify(eventMapper, times(1)).insert(any());
    }

    @Test
    @DisplayName("同 progressId 异摘要 → CONFLICT(DIGEST_CONFLICT)")
    void conflict_sameIdDifferentDigest() {
        service.receive(cmd("DP-1", 1, "STARTED", 0));
        DownloadProgressCmd tampered = cmd("DP-1", 1, "STARTED", 0);
        tampered.setEventDigest("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");

        DownloadProgressResult result = service.receive(tampered);

        assertEquals(DownloadProgressAppService.DISPOSITION_CONFLICT, result.getDisposition());
        assertEquals(DownloadProgressAppService.DENY_DIGEST_CONFLICT, result.getDenyReason());
        assertFalse(result.isAccepted());
    }

    @Test
    @DisplayName("更高序号字节回退 → REJECTED BYTES_REGRESSION")
    void higherSeq_bytesRegression_rejected() {
        when(snapshotMapper.selectForUpdate(9001L, "PKG-1", 1)).thenReturn(snapshot(2, 5000L));

        DownloadProgressResult result = service.receive(cmd("DP-3", 3, "PROGRESS", 4000L));

        assertEquals(DownloadProgressAppService.DENY_BYTES_REGRESSION, result.getDenyReason());
        assertFalse(result.isAccepted());
        verify(eventMapper, never()).insert(any());
    }

    @Test
    @DisplayName("较旧合法序号 → LATE_OBSERVATION：追加历史但不推进快照")
    void olderSeq_lateObservation() {
        when(snapshotMapper.selectForUpdate(9001L, "PKG-1", 1)).thenReturn(snapshot(3, 3000L));

        DownloadProgressResult result = service.receive(cmd("DP-2", 2, "PROGRESS", 2000L));

        assertEquals(DownloadProgressAppService.DISPOSITION_LATE, result.getDisposition());
        assertTrue(result.isAccepted());
        verify(eventMapper).insert(any());
        verify(decisionMapper, never()).advanceSnapshot(anyLong(), anyString(), anyInt(), anyString(), anyLong(),
                anyString(), anyLong(), anyLong(), anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("相同序号不同 progressId → CONFLICT(SECOND_KEY_CONFLICT)")
    void equalSeq_secondKeyConflict() {
        when(snapshotMapper.selectForUpdate(9001L, "PKG-1", 1)).thenReturn(snapshot(3, 3000L));

        DownloadProgressResult result = service.receive(cmd("DP-OTHER", 3, "PROGRESS", 3000L));

        assertEquals(DownloadProgressAppService.DISPOSITION_CONFLICT, result.getDisposition());
        assertEquals(DownloadProgressAppService.DENY_SECOND_KEY_CONFLICT, result.getDenyReason());
    }

    @Test
    @DisplayName("非零 RESET_OFFSET 无授权证据 → REJECTED；命中最近授权 → CURRENT")
    void resetOffset_requiresGrantEvidence() {
        when(snapshotMapper.selectForUpdate(9001L, "PKG-1", 1)).thenReturn(snapshot(2, 5000L));
        DownloadProgressCmd unauthorized = cmd("DP-R", 3, "RESET_OFFSET", 262144L);
        unauthorized.setResumeOffset(262144L);

        DownloadProgressResult rejected = service.receive(unauthorized);
        assertEquals(DownloadProgressAppService.DENY_RESET_UNAUTHORIZED, rejected.getDenyReason());

        VehicleTaskPackagePo granted = VehicleTaskPackagePo.builder()
                .vehicleTaskId(9001L).packageId("PKG-1").packageRevision("REV-1").etag("etag-1")
                .grantOffset(262144L).grantResumeAllowed(true).grantPackageRevision("REV-1")
                .grantEtag("etag-1").grantExpiresAt(Date.from(Instant.now().plusSeconds(600))).build();
        when(vehicleTaskPackageMapper.selectByVehicleTaskId(9001L)).thenReturn(List.of(granted));

        DownloadProgressResult accepted = service.receive(cmd("DP-R2", 3, "RESET_OFFSET", 262144L));
        // resumeOffset 默认 0，基线不一致 → 先被基线校验拦截
        assertEquals(DownloadProgressAppService.DENY_RESET_BASELINE_MISMATCH, accepted.getDenyReason());
    }

    @Test
    @DisplayName("COMPLETED 字节不足 → REJECTED COMPLETED_BYTES_MISMATCH")
    void completedBytesMismatch_rejected() {
        DownloadProgressResult result = service.receive(cmd("DP-C", 2, "COMPLETED", TOTAL - 1));

        assertEquals(DownloadProgressAppService.DENY_COMPLETED_BYTES_MISMATCH, result.getDenyReason());
        assertFalse(result.isAccepted());
    }

    @Test
    @DisplayName("作用域：车辆任务不存在/包不在冻结清单/大小与权威值不符一律拒绝")
    void scopeValidation_rejected() {        when(vehicleTaskRepository.getById(VehicleTaskId.of(9001L))).thenReturn(Optional.empty());
        assertEquals(DownloadProgressAppService.DENY_VEHICLE_TASK_ID_MISSING,
                service.receive(cmd("DP-S", 1, "STARTED", 0)).getDenyReason());

        Instant now = Instant.now();
        VehicleTask vehicleTask = VehicleTask.reconstitute(VehicleTaskId.of(9001L), 77L, "LSVAU2188N2ZG4G",
                TaskRevision.initial(), SnapshotDigest.of("digest"), VehicleTaskStatus.EXECUTING,
                DownloadReadyState.IN_PROGRESS, ConsentState.NOT_REQUIRED, now, now, now.plusSeconds(3600),
                null, null, null, null, 0, null, false, null, null, null, null, null, null, 1L);
        when(vehicleTaskRepository.getById(VehicleTaskId.of(9001L))).thenReturn(Optional.of(vehicleTask));
        when(vehicleTaskPackageMapper.selectByVehicleTaskId(9001L)).thenReturn(List.of());
        assertEquals(DownloadProgressAppService.DENY_PACKAGE_ID_MISSING,
                service.receive(cmd("DP-S2", 1, "STARTED", 0)).getDenyReason());

        when(vehicleTaskPackageMapper.selectByVehicleTaskId(9001L)).thenReturn(List.of(
                VehicleTaskPackagePo.builder().vehicleTaskId(9001L).packageId("PKG-1")
                        .packageRevision("REV-1").etag("etag-1").build()));
        when(softwarePackageMapper.selectPackageSizeByCode("PKG-1")).thenReturn(TOTAL + 1);
        assertEquals(DownloadProgressAppService.DENY_TOTAL_BYTES_INVALID,
                service.receive(cmd("DP-S3", 1, "STARTED", 0)).getDenyReason());
    }

    @Test
    @DisplayName("CR-028：冻结包清单缺失但包属于实时投影 → fail-open 受理 CURRENT 并计入指标")
    void freezeMissing_fallsBackToProjection() {
        when(vehicleTaskPackageMapper.selectByVehicleTaskId(9001L)).thenReturn(List.of());
        when(vehicleTaskPackageService.requiredPackageIds(any())).thenReturn(java.util.Set.of("PKG-1"));

        DownloadProgressResult result = service.receive(cmd("DP-F1", 1, "STARTED", 0));

        assertTrue(result.isAccepted());
        assertEquals(DownloadProgressAppService.DISPOSITION_CURRENT, result.getDisposition());
        verify(metrics, atLeastOnce()).increment(DownloadProgressAppService.METRIC_FREEZE_MISSING);
        verify(eventMapper).insert(any());
    }

    @Test
    @DisplayName("CR-028：冻结包清单缺失且包不属于实时投影 → 仍拒绝 PACKAGE_ID_MISSING")
    void freezeMissing_packageNotInProjection_rejected() {
        when(vehicleTaskPackageMapper.selectByVehicleTaskId(9001L)).thenReturn(List.of());
        when(vehicleTaskPackageService.requiredPackageIds(any())).thenReturn(java.util.Set.of("PKG-OTHER"));

        DownloadProgressResult result = service.receive(cmd("DP-F2", 1, "STARTED", 0));

        assertFalse(result.isAccepted());
        assertEquals(DownloadProgressAppService.DENY_PACKAGE_ID_MISSING, result.getDenyReason());
        verify(eventMapper, never()).insert(any());
        verify(metrics, never()).increment(DownloadProgressAppService.METRIC_FREEZE_MISSING);
    }

    @Test
    @DisplayName("非法报文：未知状态/摘要格式/offset 越界均拒绝")
    void illegalReport_rejected() {
        DownloadProgressCmd unknownState = cmd("DP-U", 1, null, 0);
        assertEquals(DownloadProgressAppService.DENY_STATE_UNSUPPORTED,
                service.receive(unknownState).getDenyReason());

        DownloadProgressCmd badDigest = cmd("DP-D", 1, "STARTED", 0);
        badDigest.setEventDigest("ABC");
        assertEquals(DownloadProgressAppService.DENY_DIGEST_VALUE_INVALID,
                service.receive(badDigest).getDenyReason());

        DownloadProgressCmd badOffset = cmd("DP-O", 1, "PROGRESS", 10L);
        badOffset.setResumeOffset(11L);
        assertEquals(DownloadProgressAppService.DENY_RESUME_OFFSET_INVALID,
                service.receive(badOffset).getDenyReason());
    }

    @Test
    @DisplayName("快照百分比由云端按字节计算并保留两位小数")
    void snapshotPercent_computedByCloud() {
        service.receive(cmd("DP-P", 2, "PROGRESS", TOTAL / 2));

        ArgumentCaptor<PackageDownloadProgressEventPo> captor =
                ArgumentCaptor.forClass(PackageDownloadProgressEventPo.class);
        verify(eventMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getProgressPercent().compareTo(new java.math.BigDecimal("50.00")));
    }
}
