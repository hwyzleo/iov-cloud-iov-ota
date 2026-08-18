package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.*;
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
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * CR-015 P1-A 车辆升级任务完整过程查询测试（§8 过程查询验收）
 * <p>覆盖：多 Execution、事件乱序与缺口、控制回执、ECU 结果、VIN 脱敏、无 payload/下载凭证。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskVehicleProcessQueryService 过程查询")
class TaskVehicleProcessQueryServiceTest {

    @Mock private TaskVehicleMapper taskVehicleMapper;
    @Mock private VehicleInventoryMapper vehicleInventoryMapper;
    @Mock private VehicleInventoryItemMapper vehicleInventoryItemMapper;
    @Mock private VehicleTaskConsentMapper vehicleTaskConsentMapper;
    @Mock private VehicleTaskPackageMapper vehicleTaskPackageMapper;
    @Mock private PackageStageResultMapper packageStageResultMapper;
    @Mock private OtaExecutionMapper otaExecutionMapper;
    @Mock private ExecutionEventMapper executionEventMapper;
    @Mock private ExecutionControlMapper executionControlMapper;
    @Mock private ExecutionControlAckMapper executionControlAckMapper;
    @Mock private ExecutionEcuResultMapper executionEcuResultMapper;
    @Mock private UpgradeLogMapper upgradeLogMapper;
    @Mock private TaskVehicleRetryLogMapper taskVehicleRetryLogMapper;
    @Mock private GatewayDeliveryObservationMapper gatewayDeliveryObservationMapper;
    @Mock private TaskAppService taskAppService;
    @Mock private ActivityAppService activityAppService;

    @InjectMocks
    private TaskVehicleProcessQueryService service;

    private TaskVehiclePo taskVehicle;

    @BeforeEach
    void setUp() {
        taskVehicle = TaskVehiclePo.builder()
                .id(10L)
                .taskId(1L)
                .activityId(100L)
                .vin("LSV0000000000000001")
                .vehicleTaskStatus("EXECUTING")
                .taskRevision(1L)
                .snapshotDigest("SNAP-1")
                .downloadReadyState("READY")
                .activeExecutionId(20L)
                .build();
    }

    private OtaExecutionPo execution(Long id, String bizId, int attemptNo, String status, Long accepted, Long finalSeq) {
        return OtaExecutionPo.builder()
                .id(id)
                .executionId(bizId)
                .vehicleTaskId(10L)
                .attemptNo(attemptNo)
                .status(status)
                .acceptedSequenceNo(accepted)
                .finalSequenceNo(finalSeq)
                .build();
    }

    private ExecutionEventPo event(String eventId, Long execId, Long seq) {
        return ExecutionEventPo.builder()
                .eventId(eventId)
                .executionId(execId)
                .sequenceNo(seq)
                .eventType("STAGE_REPORT")
                .eventDigest("D-" + seq)
                .receivedTime(Date.from(Instant.parse("2026-08-01T00:00:00Z")))
                .build();
    }

    @Test
    @DisplayName("聚合完整过程视图：多执行/缺失区间/控制/ECU/日志/投递")
    void getProcess_aggregatesAll() {
        when(taskVehicleMapper.selectPoById(10L)).thenReturn(taskVehicle);

        // 清单
        VehicleInventoryPo inventory = VehicleInventoryPo.builder()
                .vin("LSV0000000000000001").inventoryRevision(2L).digest("DIGEST-2").algorithm("SHA-256")
                .acceptedTime(Date.from(Instant.parse("2026-08-01T00:00:00Z")))
                .build();
        when(vehicleInventoryMapper.selectList(any())).thenReturn(List.of(inventory));
        when(vehicleInventoryItemMapper.selectCount(any())).thenReturn(6L);

        // 授权
        VehicleTaskConsentPo consent = VehicleTaskConsentPo.builder()
                .vehicleTaskId(10L).taskId(100L).vin("LSV0000000000000001").taskRevision(1L)
                .consentResult("GRANTED").consentReceiptId("RCPT-1")
                .articleId(3L).articleVersion("v1").articleHash("T-HASH")
                .consentScopeDigest("SCOPE-1").channel("TBOX").sourceModel("NATIVE")
                .receivedAt(Date.from(Instant.parse("2026-08-01T01:00:00Z")))
                .build();
        when(vehicleTaskConsentMapper.selectList(any())).thenReturn(List.of(consent));

        // 包 + 阶段结果
        VehicleTaskPackagePo pkg = VehicleTaskPackagePo.builder()
                .vehicleTaskId(10L).packageId("PKG-1").packageRevision("R1").etag("E1")
                .downloadState("DONE").verifyState("VERIFIED").build();
        when(vehicleTaskPackageMapper.selectList(any())).thenReturn(List.of(pkg));
        PackageStageResultPo stage = PackageStageResultPo.builder()
                .packageId("PKG-1").resultStatus("SUCCESS").signatureResult("OK").decryptResult("OK")
                .build();
        when(packageStageResultMapper.selectList(any())).thenReturn(List.of(stage));

        // 两个执行 + 事件（执行2 乱序+缺口：accepted=2, final=5, 收到 1,2,4）
        OtaExecutionPo exec1 = execution(20L, "EXEC-1", 1, "SUCCEEDED", 4L, 4L);
        OtaExecutionPo exec2 = execution(21L, "EXEC-2", 2, "INSTALLING", 2L, 5L);
        when(otaExecutionMapper.selectList(any())).thenReturn(List.of(exec1, exec2));
        when(executionEventMapper.selectList(any())).thenReturn(List.of(
                event("e1", 20L, 1L), event("e2", 20L, 2L), event("e3", 20L, 3L), event("e4", 20L, 4L),
                event("e5", 21L, 1L), event("e6", 21L, 2L), event("e7", 21L, 4L)));

        // 控制 + 回执
        ExecutionControlPo control = ExecutionControlPo.builder()
                .executionId(21L).controlId("CTL-1").controlRevision(3).action("PAUSE").scope("ALL").applyMode("SAFE")
                .build();
        when(executionControlMapper.selectList(any())).thenReturn(List.of(control));
        ExecutionControlAckPo ack = ExecutionControlAckPo.builder()
                .controlId("CTL-1").ackStatus("RECEIVED").ackSequenceNo(1)
                .ackTime(Date.from(Instant.parse("2026-08-02T00:00:00Z")))
                .build();
        when(executionControlAckMapper.selectList(any())).thenReturn(List.of(ack));

        // ECU 结果
        ExecutionEcuResultPo ecu = ExecutionEcuResultPo.builder()
                .executionId(20L).ecuId("ECU-1").targetSoftwareVersion("V1.1").actualSoftwareVersion("V1.1")
                .result("SUCCESS").build();
        when(executionEcuResultMapper.selectList(any())).thenReturn(List.of(ecu));

        // 日志
        UpgradeLogPo log = UpgradeLogPo.builder()
                .taskId(1L).vin("LSV0000000000000001").logUrl("oss://log/1").uploadState("UPLOADED")
                .uploadTime(Date.from(Instant.parse("2026-08-02T01:00:00Z")))
                .build();
        when(upgradeLogMapper.selectList(any())).thenReturn(List.of(log));

        // 技术投递观测
        GatewayDeliveryObservationPo obs = GatewayDeliveryObservationPo.builder()
                .stage("DOWNLINK").outcome("OUTCOME_ACCEPTED").reason(null).retryable(false)
                .retryAfterMs(0L).occurredAtMs(1754000000000L)
                .build();
        when(gatewayDeliveryObservationMapper.selectByVinHash(anyString(), anyInt())).thenReturn(List.of(obs));

        TaskVehicleProcessResult result = service.getProcess(10L);

        assertNotNull(result);
        assertEquals("***0001", result.getVinMasked()); // VIN 脱敏，不返回完整 VIN

        // vehicleTask
        assertEquals("EXECUTING", result.getVehicleTask().getVehicleTaskStatus());
        assertEquals(1L, result.getVehicleTask().getTaskRevision());

        // 清单
        assertEquals(2L, result.getInventorySummary().getInventoryRevision());
        assertEquals(6, result.getInventorySummary().getEcuCount());

        // 授权
        assertEquals("RCPT-1", result.getConsentSummary().getReceiptId());
        assertEquals("GRANTED", result.getConsentSummary().getConsentResult());

        // 包
        assertEquals(1, result.getPackageStages().size());
        assertEquals("SUCCESS", result.getPackageStages().get(0).getStageResultStatus());

        // 执行：执行2 缺失区间应为 "3,5"；执行1 无缺失
        assertEquals(2, result.getExecutions().size());
        ExecutionProcessView v1 = result.getExecutions().get(0);
        ExecutionProcessView v2 = result.getExecutions().get(1);
        assertEquals("", v1.getMissingSequenceRanges());
        assertTrue(v1.getActive()); // 执行1 是活动执行
        assertEquals("3,5", v2.getMissingSequenceRanges());

        // 控制
        assertEquals(3, result.getControls().getLatestControlRevision());
        assertEquals("PAUSE", result.getControls().getLatestAction());
        assertEquals("RECEIVED", result.getControls().getLatestAckStatus());

        // ECU
        assertEquals(1, result.getEcuResultSummary().size());
        assertEquals("V1.1", result.getEcuResultSummary().get(0).getActualSoftwareVersion());

        // 日志
        assertEquals(1, result.getLogSummary().size());
        assertEquals("oss://log/1", result.getLogSummary().get(0).getLogUrl());

        // 投递观测
        assertEquals(1, result.getDeliveryObservationSummary().size());
        assertEquals("OUTCOME_ACCEPTED", result.getDeliveryObservationSummary().get(0).getOutcome());
    }

    @Test
    @DisplayName("过程视图不泄露事件 payload bytes")
    void getProcess_noEventPayload() {
        when(taskVehicleMapper.selectPoById(10L)).thenReturn(taskVehicle);
        OtaExecutionPo exec = execution(20L, "EXEC-1", 1, "SUCCEEDED", 1L, 1L);
        when(otaExecutionMapper.selectList(any())).thenReturn(List.of(exec));
        ExecutionEventPo eventWithPayload = ExecutionEventPo.builder()
                .eventId("e1").executionId(20L).sequenceNo(1L).eventType("STAGE_REPORT")
                .eventPayload("{\"raw\":\"secret-payload\"}")
                .eventDigest("D-1")
                .build();
        when(executionEventMapper.selectList(any())).thenReturn(List.of(eventWithPayload));

        TaskVehicleProcessResult result = service.getProcess(10L);

        // executions 摘要只含缺失区间与水位，不含事件 payload
        assertEquals("", result.getExecutions().get(0).getMissingSequenceRanges());

        // 事件子资源只返回 digest，不返回 payload
        ExecutionEventProcessView ev = service.listExecutionEvents(20L).get(0);
        assertEquals("D-1", ev.getEventDigest());
    }

    @Test
    @DisplayName("重试/升级日志子资源 VIN 脱敏")
    void subResources_maskVin() {
        when(taskVehicleMapper.selectPoById(10L)).thenReturn(taskVehicle);
        TaskVehicleRetryLogPo retry = TaskVehicleRetryLogPo.builder()
                .taskId(1L).vin("LSV0000000000000001").stage("DOWNLOAD").attemptNo(2)
                .offset(1024L).result("RESUME").reason("网络中断").build();
        when(taskVehicleRetryLogMapper.selectList(any())).thenReturn(List.of(retry));
        UpgradeLogPo log = UpgradeLogPo.builder()
                .taskId(1L).vin("LSV0000000000000001").logUrl("oss://log/1").uploadState("UPLOADED").build();
        when(upgradeLogMapper.selectList(any())).thenReturn(List.of(log));

        List<TaskVehicleRetryLogResult> retries = service.listRetryLogs(10L, null, null, null, null, null);
        assertEquals("***0001", retries.get(0).getVinMasked());
        assertEquals(2, retries.get(0).getAttemptNo());

        List<UpgradeLogResult> logs = service.listUpgradeLogs(10L, null, null, null);
        assertEquals("***0001", logs.get(0).getVinMasked());
        assertEquals("oss://log/1", logs.get(0).getLogUrl());
    }
}
