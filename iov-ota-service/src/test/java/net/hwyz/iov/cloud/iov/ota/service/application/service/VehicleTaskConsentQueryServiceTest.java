package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskConsentCurrentResult;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.VehicleTaskConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskConsentRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.service.ConsentPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * VehicleTaskConsentQueryService 查询测试（CR-016 §6/§9）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehicleTaskConsentQueryService 授权查询")
class VehicleTaskConsentQueryServiceTest {

    @Mock private VehicleTaskRepository vehicleTaskRepository;
    @Mock private VehicleTaskConsentRepository vehicleTaskConsentRepository;

    private final ConsentPolicy consentPolicy = new ConsentPolicy();
    private final Instant now = Instant.now();

    private VehicleTaskConsentQueryService service;

    @BeforeEach
    void setUp() {
        service = new VehicleTaskConsentQueryService(vehicleTaskRepository,
                vehicleTaskConsentRepository, consentPolicy);
    }

    @Test
    @DisplayName("listHistory 返回不可变授权历史")
    void listHistory_returnsHistory() {
        VehicleTaskConsent grant = new VehicleTaskConsent()
                .setId(1L).setVehicleTaskId(10L).setTaskId(100L).setVin("VIN001")
                .setTaskRevision(1L).setResult(ConsentResult.GRANTED)
                .setConsentReceiptId("RCPT-1").setArticleId(200L).setArticleVersion("v1")
                .setArticleHash("hash").setConsentScopeDigest("scope").setChannel("TBOX")
                .setReceivedAt(now).setSourceModel("NATIVE");
        when(vehicleTaskConsentRepository.findByVehicleTaskId(10L)).thenReturn(List.of(grant));

        List<VehicleTaskConsentResult> history = service.listHistory(10L);

        assertEquals(1, history.size());
        VehicleTaskConsentResult r = history.get(0);
        assertEquals(1L, r.getConsentRecordId());
        assertEquals("GRANTED", r.getConsentResult());
        assertEquals("RCPT-1", r.getConsentReceiptId());
        assertEquals(1L, r.getTaskRevision());
        assertEquals("hash", r.getArticleHash());
        assertEquals("TBOX", r.getChannel());
        assertEquals("NATIVE", r.getSourceModel());
    }

    @Test
    @DisplayName("getCurrent 返回当前权威状态与有效判定")
    void getCurrent_returnsValidCurrent() {
        VehicleTask vt = VehicleTask.create(
                VehicleTaskId.of(10L), 100L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("digest"),
                now.minusSeconds(60), now.plusSeconds(60), now.plusSeconds(3600),
                true, 200L, "v1", "terms-hash");
        vt.markVisible(now);
        vt.enterConsentPending();
        vt.applyConsent(ConsentResult.GRANTED, 5L, "scope-1", now, false);

        VehicleTaskConsent current = new VehicleTaskConsent()
                .setId(5L).setVehicleTaskId(10L).setTaskId(100L).setVin("VIN001")
                .setTaskRevision(1L).setResult(ConsentResult.GRANTED)
                .setConsentReceiptId("RCPT-1")
                .setArticleHash("terms-hash").setConsentScopeDigest("scope-1");

        when(vehicleTaskRepository.getById(VehicleTaskId.of(10L))).thenReturn(Optional.of(vt));
        when(vehicleTaskConsentRepository.findCurrentByVehicleTaskId(10L)).thenReturn(Optional.of(current));

        VehicleTaskConsentCurrentResult r = service.getCurrent(10L);

        assertEquals(10L, r.getVehicleTaskId());
        assertEquals(ConsentState.GRANTED.getValue(), r.getConsentState());
        assertEquals(5L, r.getCurrentConsentId());
        assertEquals("RCPT-1", r.getCurrentReceiptId());
        assertTrue(r.isValid());
        assertNull(r.getInvalidReason());
    }

    @Test
    @DisplayName("getCurrent 凭据失效时返回原因")
    void getCurrent_returnsInvalidReason() {
        VehicleTask vt = VehicleTask.create(
                VehicleTaskId.of(10L), 100L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("digest"),
                now.minusSeconds(60), now.plusSeconds(60), now.plusSeconds(3600),
                true, 200L, "v1", "terms-hash");
        vt.markVisible(now);
        vt.enterConsentPending();
        vt.applyConsent(ConsentResult.GRANTED, 5L, "scope-1", now, false);
        // 修订升级使凭据失效
        vt.upgradeRevision(TaskRevision.initial().next(), SnapshotDigest.of("d2"), now);

        VehicleTaskConsent stale = new VehicleTaskConsent()
                .setId(5L).setVehicleTaskId(10L).setTaskId(100L).setVin("VIN001")
                .setTaskRevision(1L).setResult(ConsentResult.GRANTED)
                .setConsentReceiptId("RCPT-1")
                .setArticleHash("terms-hash").setConsentScopeDigest("scope-1");

        when(vehicleTaskRepository.getById(VehicleTaskId.of(10L))).thenReturn(Optional.of(vt));
        when(vehicleTaskConsentRepository.findCurrentByVehicleTaskId(10L)).thenReturn(Optional.of(stale));

        VehicleTaskConsentCurrentResult r = service.getCurrent(10L);

        assertFalse(r.isValid());
        assertNotNull(r.getInvalidReason());
    }
}
