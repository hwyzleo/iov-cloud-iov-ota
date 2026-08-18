package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.DownloadReadyState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.ExecutionId;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.TaskVehiclePo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VehicleTaskConverter 转换器测试（CR-012）
 *
 * @author hwyz_leo
 */
@DisplayName("VehicleTaskConverter PO <-> Domain 转换")
class VehicleTaskConverterTest {

    private final VehicleTaskConverter converter = new VehicleTaskConverter();

    @Test
    @DisplayName("toPo 应正确映射聚合字段到 PO")
    void toPo_shouldMapAggregateFields() {
        Instant releaseAt = Instant.now().minusSeconds(60);
        Instant startTime = Instant.now();
        Instant endTime = Instant.now().plusSeconds(3600);

        VehicleTask vt = VehicleTask.create(
                VehicleTaskId.of(1L), 100L, "VIN001",
                TaskRevision.of(3L), SnapshotDigest.of("digest123"),
                releaseAt, startTime, endTime);
        vt.markVisible(Instant.now());
        vt.enterConsentPending();
        vt.applyConsent(ConsentResult.GRANTED, 1L, "scope", Instant.now(), false);
        vt.setConsentState(ConsentState.GRANTED);
        vt.setLocalDisposition("DEFER");
        vt.setPackageCacheAction("KEEP");

        TaskVehiclePo po = converter.toPo(vt);

        assertEquals(1L, po.getId());
        assertEquals(100L, po.getTaskId());
        assertEquals("VIN001", po.getVin());
        assertEquals(VehicleTaskStatus.READY_TO_INSTALL.getValue(), po.getVehicleTaskStatus());
        assertEquals(3L, po.getTaskRevision());
        assertEquals("digest123", po.getSnapshotDigest());
        assertEquals(ConsentState.GRANTED.getValue(), po.getConsentState());
        assertEquals("DEFER", po.getLocalDisposition());
        assertEquals("KEEP", po.getPackageCacheAction());
    }

    @Test
    @DisplayName("toDomain 应正确重建聚合（不触发事件）")
    void toDomain_shouldReconstituteAggregate() {
        TaskVehiclePo po = TaskVehiclePo.builder()
                .id(1L)
                .taskId(100L)
                .vin("VIN001")
                .vehicleTaskStatus(VehicleTaskStatus.EXECUTING.getValue())
                .taskRevision(3L)
                .snapshotDigest("digest123")
                .downloadReadyState(DownloadReadyState.VERIFIED.getValue())
                .consentState(ConsentState.GRANTED.getValue())
                .releaseAt(Date.from(Instant.now().minusSeconds(60)))
                .vtStartTime(Date.from(Instant.now()))
                .vtEndTime(Date.from(Instant.now().plusSeconds(3600)))
                .activeExecutionId(5L)
                .lastAttemptNo(1)
                .build();

        VehicleTask vt = converter.toDomain(po);

        assertNotNull(vt);
        assertEquals(1L, vt.getId().getValue());
        assertEquals(100L, vt.getTaskId());
        assertEquals("VIN001", vt.getVin());
        assertEquals(VehicleTaskStatus.EXECUTING, vt.getStatus());
        assertEquals(3L, vt.getTaskRevision().getValue());
        assertEquals("digest123", vt.getSnapshotDigest().getValue());
        assertEquals(DownloadReadyState.VERIFIED, vt.getDownloadReadyState());
        assertEquals(ConsentState.GRANTED, vt.getConsentState());
        assertEquals(5L, vt.getActiveExecutionId().getValue());
        assertEquals(1, vt.getLastAttemptNo());
        assertTrue(vt.getPendingEvents().isEmpty());
    }

    @Test
    @DisplayName("toDomain null PO 返回 null")
    void toDomain_nullPo_returnsNull() {
        assertNull(converter.toDomain(null));
    }

    @Test
    @DisplayName("toPo/toDomain 往返保持核心字段一致")
    void roundTrip_shouldPreserveCoreFields() {
        VehicleTask original = VehicleTask.create(
                VehicleTaskId.of(2L), 200L, "VIN002",
                TaskRevision.of(5L), SnapshotDigest.of("roundTripDigest"),
                Instant.now().minusSeconds(60), Instant.now(), Instant.now().plusSeconds(3600));
        original.markVisible(Instant.now());
        original.enterConsentPending();
        original.applyConsent(ConsentResult.GRANTED, 1L, "scope", Instant.now(), true);
        original.startDownload();
        original.markDownloadReady();

        TaskVehiclePo po = converter.toPo(original);
        VehicleTask restored = converter.toDomain(po);

        assertEquals(original.getId().getValue(), restored.getId().getValue());
        assertEquals(original.getTaskId(), restored.getTaskId());
        assertEquals(original.getVin(), restored.getVin());
        assertEquals(original.getStatus(), restored.getStatus());
        assertEquals(original.getTaskRevision().getValue(), restored.getTaskRevision().getValue());
        assertEquals(original.getSnapshotDigest().getValue(), restored.getSnapshotDigest().getValue());
        assertEquals(original.getDownloadReadyState(), restored.getDownloadReadyState());
        assertEquals(original.getConsentState(), restored.getConsentState());
    }
}
