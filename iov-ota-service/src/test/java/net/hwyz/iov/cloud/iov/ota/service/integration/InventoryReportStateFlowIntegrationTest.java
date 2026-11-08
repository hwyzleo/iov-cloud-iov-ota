package net.hwyz.iov.cloud.iov.ota.service.integration;

import net.hwyz.iov.cloud.iov.ota.api.vo.VehicleMpt;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.InventoryDisposition;
import net.hwyz.iov.cloud.iov.ota.service.adapter.web.assembler.VehicleProjectionMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.InventoryItemCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleManagementResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.InventoryObservedOutboxProducer;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.iov.ota.service.application.service.VehicleInventoryApplicationService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehStatusMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryItemMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventoryReportStateMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleInventorySoftwareUnitMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleProjectionMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryItemPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventoryReportStatePo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleInventorySoftwareUnitPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleProjectionPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CR-022 集成测试：清单上报 → 运行态 → 升级车辆管理查询全链路
 *
 * <p>使用真实 {@link VehicleInventoryApplicationService} / {@link VehicleAppService} /
 * {@link VehicleProjectionMptAssembler}，以内存表模拟 Mapper 语义（唯一键 + GREATEST 单调更新、
 * 主档 LEFT JOIN 运行态）。覆盖 §7.1/§7.2 与 §7.3 的端到端回归要点。
 *
 * <p>说明：同 message_id 技术重投由 Inbox 在调用业务服务前短路
 * （{@code FotaKafkaInboundHandler}），不在本用例重复验证。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CR-022 集成测试 - 清单上报运行态与管理查询")
class InventoryReportStateFlowIntegrationTest {

    private static final String VIN = "LSVAU2188N2ZG4G";
    private static final String VIN_NO_STATE = "LSVAU2188N2ZG4H";

    @Mock private VehicleInventoryMapper vehicleInventoryMapper;
    @Mock private VehicleInventoryItemMapper vehicleInventoryItemMapper;
    @Mock private VehicleInventorySoftwareUnitMapper vehicleInventorySoftwareUnitMapper;
    @Mock private InventoryObservedOutboxProducer observedOutboxProducer;
    @Mock private VehicleProjectionMapper vehicleProjectionMapper;
    @Mock private VehStatusMapper vehStatusMapper;

    private InMemoryReportStateMapper reportStateMapper;

    private VehicleInventoryApplicationService inventoryService;
    private VehicleAppService vehicleAppService;

    /** 内存清单头：key = vin#revision */
    private final Map<String, VehicleInventoryPo> inventoryStore = new HashMap<>();
    private final Map<Long, List<VehicleInventoryItemPo>> itemStore = new HashMap<>();
    private final Map<Long, List<VehicleInventorySoftwareUnitPo>> unitStore = new HashMap<>();
    private final Map<String, VehicleProjectionPo> projectionStore = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);
    private int observedOutboxCount;

    @BeforeEach
    void setUp() {
        inventoryStore.clear();
        itemStore.clear();
        unitStore.clear();
        projectionStore.clear();
        observedOutboxCount = 0;
        reportStateMapper = new InMemoryReportStateMapper();

        when(vehicleInventoryMapper.selectByVinAndRevision(anyString(), anyLong()))
                .thenAnswer(inv -> inventoryStore.get(key(inv.getArgument(0), inv.getArgument(1))));
        when(vehicleInventoryMapper.selectLatestByVin(anyString()))
                .thenAnswer(inv -> latestInventory(inv.getArgument(0)));
        when(vehicleInventoryMapper.insert(any(VehicleInventoryPo.class))).thenAnswer(inv -> {
            VehicleInventoryPo po = inv.getArgument(0);
            po.setId(idSeq.getAndIncrement());
            inventoryStore.put(key(po.getVin(), po.getInventoryRevision()), po);
            return 1;
        });
        when(vehicleInventoryItemMapper.insert(any(VehicleInventoryItemPo.class))).thenAnswer(inv -> {
            VehicleInventoryItemPo po = inv.getArgument(0);
            po.setId(idSeq.getAndIncrement());
            itemStore.computeIfAbsent(po.getInventoryId(), k -> new ArrayList<>()).add(po);
            return 1;
        });
        when(vehicleInventoryItemMapper.selectByInventoryId(anyLong()))
                .thenAnswer(inv -> itemStore.getOrDefault(inv.getArgument(0), List.of()));
        when(vehicleInventorySoftwareUnitMapper.insert(any(VehicleInventorySoftwareUnitPo.class)))
                .thenAnswer(inv -> {
                    VehicleInventorySoftwareUnitPo po = inv.getArgument(0);
                    po.setId(idSeq.getAndIncrement());
                    unitStore.computeIfAbsent(po.getInventoryId(), k -> new ArrayList<>()).add(po);
                    return 1;
                });
        when(vehicleInventorySoftwareUnitMapper.selectByInventoryItemId(anyLong()))
                .thenAnswer(inv -> List.of());
        when(observedOutboxProducer.produce(any(), any(), any(), any())).thenAnswer(inv -> {
            observedOutboxCount++;
            return true;
        });

        // 主档投影 + 运行态 LEFT JOIN（一次查询，非逐 VIN 查询）
        when(vehicleProjectionMapper.selectManagementList(any(), any(), any()))
                .thenAnswer(inv -> projectionStore.values().stream()
                        .filter(p -> inv.getArgument(0) == null
                                || p.getVin().contains((String) inv.getArgument(0)))
                        .sorted(Comparator.comparing(VehicleProjectionPo::getId).reversed())
                        .map(this::toManagementResult)
                        .collect(Collectors.toList()));
        when(vehicleProjectionMapper.selectManagementByVin(anyString()))
                .thenAnswer(inv -> {
                    VehicleProjectionPo po = projectionStore.get(inv.getArgument(0));
                    return po == null ? null : toManagementResult(po);
                });

        inventoryService = new VehicleInventoryApplicationService(
                vehicleInventoryMapper, vehicleInventoryItemMapper,
                vehicleInventorySoftwareUnitMapper, reportStateMapper, observedOutboxProducer);
        vehicleAppService = new VehicleAppService(vehStatusMapper, vehicleProjectionMapper);

        projectionStore.put(VIN, projection(VIN));
        projectionStore.put(VIN_NO_STATE, projection(VIN_NO_STATE));
    }

    // ------------------------------------------------------------ scenarios

    @Test
    @DisplayName("首次 FULL → 运行态建立 → 管理详情返回 lastInventoryReportedAt")
    void first_full_creates_report_state_and_detail_shows_it() {
        VehicleInventoryApplicationService.InventoryHandlingResult result =
                inventoryService.handleInventory(fullCmd(1L, "digest-1"));

        assertEquals(InventoryDisposition.ACCEPTED, result.getDisposition());
        Date lastReportedAt = reportStateMapper.store.get(VIN);
        assertNotNull(lastReportedAt);
        assertEquals(1, observedOutboxCount);

        VehicleMpt detail = VehicleProjectionMptAssembler.INSTANCE
                .fromResult(vehicleAppService.getVehicleByVin(VIN));
        assertEquals(lastReportedAt, detail.getLastInventoryReportedAt());
        // 列表同样一次关联返回
        List<VehicleManagementResult> rows = vehicleAppService.search(null, null, null);
        VehicleManagementResult row = rows.stream().filter(r -> VIN.equals(r.getVin())).findFirst().orElseThrow();
        assertEquals(lastReportedAt, row.getLastInventoryReportedAt());
    }

    @Test
    @DisplayName("FULL 业务幂等重放 → 刷新运行态，accepted_time 与观测 Outbox 不变")
    void full_replay_advances_report_state_only() {
        inventoryService.handleInventory(fullCmd(1L, "digest-1"));
        VehicleInventoryPo header = inventoryStore.get(key(VIN, 1L));
        Date acceptedTime = header.getAcceptedTime();
        int outboxAfterFirst = observedOutboxCount;
        assertEquals(1, outboxAfterFirst);

        // 模拟历史运行态（证明本次重放推进）
        Date historical = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
        reportStateMapper.store.put(VIN, historical);

        // 新 message_id、同 VIN + revision + digest 的 FULL 业务幂等重放
        VehicleInventoryApplicationService.InventoryHandlingResult replay =
                inventoryService.handleInventory(fullCmd(1L, "digest-1"));

        assertEquals(InventoryDisposition.ACCEPTED, replay.getDisposition());
        assertEquals(acceptedTime, inventoryStore.get(key(VIN, 1L)).getAcceptedTime());
        assertEquals(outboxAfterFirst, observedOutboxCount);
        assertTrue(reportStateMapper.store.get(VIN).after(historical));
    }

    @Test
    @DisplayName("DIGEST 命中 → 刷新运行态，清单与观测 Outbox 不变")
    void digest_hit_advances_report_state_only() {
        inventoryService.handleInventory(fullCmd(1L, "digest-1"));
        VehicleInventoryPo header = inventoryStore.get(key(VIN, 1L));
        Date acceptedTime = header.getAcceptedTime();
        int outboxAfterFull = observedOutboxCount;
        int itemCount = itemStore.get(header.getId()).size();

        Date historical = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
        reportStateMapper.store.put(VIN, historical);

        InventoryDisposition disposition = inventoryService.handleInventory(digestCmd(1L, "digest-1"))
                .getDisposition();

        assertEquals(InventoryDisposition.ACCEPTED, disposition);
        assertEquals(acceptedTime, inventoryStore.get(key(VIN, 1L)).getAcceptedTime());
        assertEquals(outboxAfterFull, observedOutboxCount);
        assertEquals(itemCount, itemStore.get(header.getId()).size());
        assertTrue(reportStateMapper.store.get(VIN).after(historical));
    }

    @Test
    @DisplayName("revision 冲突 → 不刷新运行态")
    void revision_conflict_does_not_advance_report_state() {
        inventoryService.handleInventory(fullCmd(1L, "digest-1"));
        Date before = reportStateMapper.store.get(VIN);

        VehicleInventoryApplicationService.InventoryHandlingResult conflict =
                inventoryService.handleInventory(fullCmd(1L, "digest-other"));

        assertEquals(InventoryDisposition.REVISION_CONFLICT, conflict.getDisposition());
        assertEquals(before, reportStateMapper.store.get(VIN));
    }

    @Test
    @DisplayName("非法明细 → 不刷新运行态")
    void invalid_payload_does_not_advance_report_state() {
        inventoryService.handleInventory(fullCmd(1L, "digest-1"));
        Date before = reportStateMapper.store.get(VIN);

        // 无清单明细 → FULL_REQUIRED
        DetectionCmd empty = fullCmd(2L, "digest-2");
        empty.setInventoryItems(List.of());
        assertEquals(InventoryDisposition.FULL_REQUIRED,
                inventoryService.handleInventory(empty).getDisposition());

        assertEquals(before, reportStateMapper.store.get(VIN));
    }

    @Test
    @DisplayName("并发/乱序 upsert → 同 VIN 仅一行，最终取最大服务端受理时刻")
    void out_of_order_upsert_keeps_max() {
        Date newer = Date.from(Instant.now());
        Date older = Date.from(Instant.now().minus(2, ChronoUnit.HOURS));

        reportStateMapper.upsertReported(VIN, newer);
        reportStateMapper.upsertReported(VIN, older);

        assertEquals(1, reportStateMapper.store.size());
        assertEquals(newer, reportStateMapper.store.get(VIN));
    }

    @Test
    @DisplayName("管理查询：无运行态记录返回 null，且列表一次关联无 N+1")
    void management_query_null_when_no_report_state_and_no_n_plus_one() {
        inventoryService.handleInventory(fullCmd(1L, "digest-1"));

        List<VehicleManagementResult> rows = vehicleAppService.search(null, null, null);
        assertEquals(2, rows.size());
        Map<String, Date> reported = rows.stream()
                .collect(Collectors.toMap(VehicleManagementResult::getVin,
                        r -> r.getLastInventoryReportedAt() == null ? new Date(0) : r.getLastInventoryReportedAt()));
        assertTrue(reported.containsKey(VIN));
        assertFalse(reportStateMapper.store.containsKey(VIN_NO_STATE));
        assertNull(rows.stream().filter(r -> VIN_NO_STATE.equals(r.getVin())).findFirst().orElseThrow()
                .getLastInventoryReportedAt());

        verify(vehicleProjectionMapper, times(1)).selectManagementList(null, null, null);
        verify(vehicleProjectionMapper, never()).selectManagementByVin(anyString());
        verify(vehicleInventoryMapper, never()).selectByVinAndRevision(eq(VIN_NO_STATE), anyLong());
    }

    // ------------------------------------------------------------ fixtures

    private VehicleInventoryPo latestInventory(String vin) {
        return inventoryStore.values().stream()
                .filter(p -> vin.equals(p.getVin()))
                .max(Comparator.comparing(VehicleInventoryPo::getInventoryRevision))
                .orElse(null);
    }

    private static String key(String vin, Long revision) {
        return vin + "#" + revision;
    }

    private static VehicleProjectionPo projection(String vin) {
        return VehicleProjectionPo.builder()
                .id(Long.valueOf(Math.abs(vin.hashCode())))
                .vin(vin).plantCode("P1").configurationCode("CFG1")
                .sourceEventId("evt").sourceVersion(1L)
                .productionTime(new Date())
                .build();
    }

    private VehicleManagementResult toManagementResult(VehicleProjectionPo po) {
        return VehicleManagementResult.builder()
                .id(po.getId()).vin(po.getVin())
                .productionTime(po.getProductionTime())
                .plantCode(po.getPlantCode()).brandCode(po.getBrandCode())
                .platformCode(po.getPlatformCode()).carLineCode(po.getCarLineCode())
                .modelCode(po.getModelCode()).variantCode(po.getVariantCode())
                .configurationCode(po.getConfigurationCode())
                .sourceEventId(po.getSourceEventId()).sourceVersion(po.getSourceVersion())
                .sourceEventTime(po.getSourceEventTime()).lastSyncTime(po.getLastSyncTime())
                .createTime(po.getCreateTime())
                .lastInventoryReportedAt(reportStateMapper.store.get(po.getVin()))
                .build();
    }

    private static DetectionCmd fullCmd(Long revision, String digest) {
        return DetectionCmd.builder()
                .vin(VIN)
                .inventoryMode("FULL")
                .inventoryRevision(revision)
                .canonicalizationVersion(1)
                .inventoryDigest(digest)
                .collectedAt(1700000000000L)
                .inventoryItems(List.of(InventoryItemCmd.builder()
                        .ecuId("ECU1")
                        .softwarePn("SPN1").softwareVersion("V1.0")
                        .hardwarePn("HPN1").hardwareVersion("H1")
                        .build()))
                .build();
    }

    private static DetectionCmd digestCmd(Long revision, String digest) {
        return DetectionCmd.builder()
                .vin(VIN)
                .inventoryMode("DIGEST")
                .inventoryRevision(revision)
                .canonicalizationVersion(1)
                .inventoryDigest(digest)
                .digestAlgorithm("SHA-256")
                .build();
    }

    /**
     * 内存版运行态 Mapper：唯一键 + GREATEST 单调更新语义。
     */
    private static final class InMemoryReportStateMapper implements VehicleInventoryReportStateMapper {

        private final Map<String, Date> store = new ConcurrentHashMap<>();

        @Override
        public int upsertReported(String vin, Date reportedAt) {
            store.merge(vin, reportedAt, (current, incoming) -> current.after(incoming) ? current : incoming);
            return 1;
        }

        @Override
        public VehicleInventoryReportStatePo selectByVin(String vin) {
            Date lastReportedAt = store.get(vin);
            return lastReportedAt == null ? null : VehicleInventoryReportStatePo.builder()
                    .vin(vin).lastReportedAt(lastReportedAt).build();
        }
    }
}
