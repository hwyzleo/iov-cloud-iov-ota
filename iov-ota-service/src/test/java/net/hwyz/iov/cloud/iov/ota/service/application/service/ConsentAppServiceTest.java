package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ConsentCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskConsentMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.VehicleTaskConsentPo;
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
 * ConsentAppService 测试（CR-012 §5.3、US-077）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConsentAppService 用户授权")
class ConsentAppServiceTest {

    @Mock private VehicleTaskRepository vehicleTaskRepository;
    @Mock private VehicleTaskConsentMapper vehicleTaskConsentMapper;

    @InjectMocks
    private ConsentAppService service;

    private VehicleTask vehicleTask;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        vehicleTask = VehicleTask.create(
                VehicleTaskId.of(1L), 100L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("digest"),
                now.minusSeconds(60), now.plusSeconds(60), now.plusSeconds(3600));
        vehicleTask.markVisible(now);
        vehicleTask.enterConsentPending();
        when(vehicleTaskRepository.getById(VehicleTaskId.of(1L))).thenReturn(Optional.of(vehicleTask));
    }

    @Test
    @DisplayName("GRANT 授权 -> 状态转为 DOWNLOAD_PENDING")
    void handleConsent_grant_transitionsToDownloadPending() {
        ConsentCmd cmd = ConsentCmd.builder()
                .vehicleTaskId(1L)
                .action("GRANT")
                .termsId(200L)
                .termsHash("terms-hash")
                .consentScopeDigest("scope-digest")
                .consentReceiptId("receipt-001")
                .vin("VIN001")
                .build();

        ConsentResult result = service.handleConsent(cmd);

        assertEquals(ConsentState.GRANTED.getValue(), result.getConsentState());
        assertTrue(result.isAccepted());
        assertEquals("receipt-001", result.getConsentReceiptId());
        assertEquals(VehicleTaskStatus.DOWNLOAD_PENDING, vehicleTask.getStatus());
        assertEquals(ConsentState.GRANTED, vehicleTask.getConsentState());
        verify(vehicleTaskConsentMapper).insert(any(VehicleTaskConsentPo.class));
        verify(vehicleTaskRepository).save(any());
    }

    @Test
    @DisplayName("DENY 授权 -> 状态保持，consentState=DENIED")
    void handleConsent_deny_setsDenied() {
        ConsentCmd cmd = ConsentCmd.builder()
                .vehicleTaskId(1L)
                .action("DENY")
                .termsId(200L)
                .termsHash("terms-hash")
                .consentScopeDigest("scope-digest")
                .consentReceiptId("receipt-002")
                .vin("VIN001")
                .build();

        ConsentResult result = service.handleConsent(cmd);

        assertEquals(ConsentState.DENIED.getValue(), result.getConsentState());
        assertFalse(result.isAccepted());
        assertEquals(ConsentState.DENIED, vehicleTask.getConsentState());
    }

    @Test
    @DisplayName("REVOKE 授权 -> consentState=REVOKED")
    void handleConsent_revoke_setsRevoked() {
        // 先授权
        vehicleTask.grantConsent(true);
        assertEquals(VehicleTaskStatus.DOWNLOAD_PENDING, vehicleTask.getStatus());

        ConsentCmd cmd = ConsentCmd.builder()
                .vehicleTaskId(1L)
                .action("REVOKE")
                .consentReceiptId("receipt-003")
                .vin("VIN001")
                .build();

        ConsentResult result = service.handleConsent(cmd);

        assertEquals(ConsentState.REVOKED.getValue(), result.getConsentState());
        assertFalse(result.isAccepted());
        assertEquals(ConsentState.REVOKED, vehicleTask.getConsentState());
    }

    @Test
    @DisplayName("未知授权动作抛异常")
    void handleConsent_unknownAction_throwsException() {
        ConsentCmd cmd = ConsentCmd.builder()
                .vehicleTaskId(1L)
                .action("UNKNOWN")
                .vin("VIN001")
                .build();

        assertThrows(net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleTaskStateException.class,
                () -> service.handleConsent(cmd));
    }
}
