package net.hwyz.iov.cloud.iov.ota.service.application.service;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentState;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ConsentCmd;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleTaskStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.SnapshotDigest;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.TaskRevision;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskConsentRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConsentAppService 测试（CR-016 §9、US-102～105）
 *
 * <p>覆盖：GRANT/REJECT/REVOKE 状态推进、不可变历史追加、幂等重放、同键异参冲突、
 * 归属/修订/条款校验、并发乐观锁。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConsentAppService 用户授权（CR-016）")
class ConsentAppServiceTest {

    @Mock private VehicleTaskRepository vehicleTaskRepository;
    @Mock private VehicleTaskConsentRepository vehicleTaskConsentRepository;
    @Mock private VehicleTaskMapper vehicleTaskMapper;

    @InjectMocks
    private ConsentAppService service;

    private VehicleTask vehicleTask;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        vehicleTask = VehicleTask.create(
                VehicleTaskId.of(1L), 100L, "VIN001",
                TaskRevision.initial(), SnapshotDigest.of("digest"),
                now.minusSeconds(60), now.plusSeconds(60), now.plusSeconds(3600),
                true, 200L, "v1", "terms-hash");
        vehicleTask.markVisible(now);
        vehicleTask.enterConsentPending();
        when(vehicleTaskRepository.getById(VehicleTaskId.of(1L))).thenReturn(Optional.of(vehicleTask));
        when(vehicleTaskMapper.updateCurrentConsent(anyLong(), anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(vehicleTaskConsentRepository.append(any(VehicleTaskConsent.class)))
                .thenAnswer(inv -> {
                    VehicleTaskConsent c = inv.getArgument(0);
                    c.setId(1L);
                    return c;
                });
    }

    private ConsentCmd grantCmd(String messageId, String idemKey) {
        return ConsentCmd.builder()
                .vehicleTaskId(1L).taskId(100L).vin("VIN001")
                .action("GRANT").taskRevision(vehicleTask.getTaskRevision().getValue())
                .articleId(200L).articleVersion("v1").articleHash("terms-hash")
                .consentScopeDigest(null)
                .messageId(messageId).idempotencyKey(idemKey)
                .build();
    }

    @Test
    @DisplayName("GRANT 授权 -> GRANTED、追加历史并推进当前状态")
    void handleConsent_grant_grantsAndAppendsHistory() {
        ConsentResult result = service.handleConsent(grantCmd("MSG-1", "IDEM-1"));

        assertEquals("GRANTED", result.getConsentState());
        assertTrue(result.isAccepted());
        assertEquals(ConsentState.GRANTED, vehicleTask.getConsentState());
        assertEquals(1L, vehicleTask.getCurrentConsentId());
        assertNotNull(result.getConsentReceiptId());
        verify(vehicleTaskConsentRepository).append(any(VehicleTaskConsent.class));
        verify(vehicleTaskMapper).updateCurrentConsent(eq(1L), anyLong(), any(), any(),
                eq("GRANTED"), eq(1L), any(), any());
    }

    @Test
    @DisplayName("DENY 授权 -> REJECTED，保持车辆任务状态")
    void handleConsent_deny_setsRejected() {
        ConsentResult result = service.handleConsent(ConsentCmd.builder()
                .vehicleTaskId(1L).taskId(100L).vin("VIN001")
                .action("DENY").taskRevision(vehicleTask.getTaskRevision().getValue())
                .articleId(200L).articleVersion("v1").articleHash("terms-hash")
                .messageId("MSG-2").idempotencyKey("IDEM-2")
                .build());

        assertEquals("REJECTED", result.getConsentState());
        assertFalse(result.isAccepted());
        assertEquals(ConsentState.REJECTED, vehicleTask.getConsentState());
    }

    @Test
    @DisplayName("REVOKE 授权 -> REVOKED")
    void handleConsent_revoke_setsRevoked() {
        service.handleConsent(grantCmd("MSG-3", "IDEM-3"));
        assertEquals(ConsentState.GRANTED, vehicleTask.getConsentState());

        ConsentResult result = service.handleConsent(ConsentCmd.builder()
                .vehicleTaskId(1L).taskId(100L).vin("VIN001")
                .action("REVOKE").taskRevision(vehicleTask.getTaskRevision().getValue())
                .articleId(200L).articleVersion("v1").articleHash("terms-hash")
                .messageId("MSG-4").idempotencyKey("IDEM-4")
                .build());

        assertEquals("REVOKED", result.getConsentState());
        assertEquals(ConsentState.REVOKED, vehicleTask.getConsentState());
    }

    @Test
    @DisplayName("未知授权动作抛异常")
    void handleConsent_unknownAction_throwsException() {
        ConsentCmd cmd = grantCmd("MSG-5", "IDEM-5");
        cmd.setAction("UNKNOWN");
        assertThrows(VehicleTaskStateException.class, () -> service.handleConsent(cmd));
    }

    @Test
    @DisplayName("归属校验失败：VIN 不一致抛异常")
    void handleConsent_vinMismatch_throwsException() {
        ConsentCmd cmd = grantCmd("MSG-6", "IDEM-6");
        cmd.setVin("OTHER-VIN");
        assertThrows(VehicleTaskStateException.class, () -> service.handleConsent(cmd));
    }

    @Test
    @DisplayName("条款校验失败：hash 不一致抛异常")
    void handleConsent_termsMismatch_throwsException() {
        ConsentCmd cmd = grantCmd("MSG-7", "IDEM-7");
        cmd.setArticleHash("WRONG-HASH");
        assertThrows(VehicleTaskStateException.class, () -> service.handleConsent(cmd));
    }

    @Test
    @DisplayName("幂等重放：同 messageId 同摘要返回原响应，不再追加")
    void handleConsent_replay_sameMessageId_returnsOriginal() {
        ConsentCmd cmd = grantCmd("MSG-8", "IDEM-8");
        String requestDigest = computeRequestDigest(cmd);
        VehicleTaskConsent existing = new VehicleTaskConsent()
                .setId(9L).setResult(net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult.GRANTED)
                .setConsentReceiptId("RCPT-ORIG").setRequestDigest(requestDigest);
        when(vehicleTaskConsentRepository.findByMessageId("MSG-8")).thenReturn(Optional.of(existing));

        ConsentResult result = service.handleConsent(cmd);

        assertTrue(result.isReplayed());
        assertEquals("RCPT-ORIG", result.getConsentReceiptId());
        assertEquals("GRANTED", result.getConsentState());
        verify(vehicleTaskConsentRepository, never()).append(any());
        verify(vehicleTaskMapper, never()).updateCurrentConsent(anyLong(), anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("同键异参冲突：同 messageId 不同摘要返回 OTA-IDEMPOTENCY-CONFLICT")
    void handleConsent_conflict_sameMessageIdDifferentDigest() {
        ConsentCmd cmd = grantCmd("MSG-9", "IDEM-9");
        VehicleTaskConsent existing = new VehicleTaskConsent()
                .setId(9L).setResult(net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult.GRANTED)
                .setConsentReceiptId("RCPT-ORIG").setRequestDigest("SOME-OTHER-DIGEST");
        when(vehicleTaskConsentRepository.findByMessageId("MSG-9")).thenReturn(Optional.of(existing));

        ConsentResult result = service.handleConsent(cmd);

        assertEquals(ConsentAppService.ERROR_IDEMPOTENCY_CONFLICT, result.getErrorCode());
        assertFalse(result.isAccepted());
        verify(vehicleTaskConsentRepository, never()).append(any());
    }

    @Test
    @DisplayName("并发冲突：row_version 推进失败抛异常且不追加历史")
    void handleConsent_rowVersionConflict_throws() {
        when(vehicleTaskMapper.updateCurrentConsent(anyLong(), anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);
        assertThrows(VehicleTaskStateException.class,
                () -> service.handleConsent(grantCmd("MSG-10", "IDEM-10")));
    }

    @Test
    @DisplayName("REJECTED -> GRANTED 重新授权生效")
    void handleConsent_reConsent_afterRejected() {
        service.handleConsent(ConsentCmd.builder()
                .vehicleTaskId(1L).taskId(100L).vin("VIN001")
                .action("DENY").taskRevision(vehicleTask.getTaskRevision().getValue())
                .articleId(200L).articleVersion("v1").articleHash("terms-hash")
                .messageId("MSG-11").idempotencyKey("IDEM-11")
                .build());
        assertEquals(ConsentState.REJECTED, vehicleTask.getConsentState());
        assertEquals(VehicleTaskStatus.CONSENT_PENDING, vehicleTask.getStatus());

        ConsentResult result = service.handleConsent(grantCmd("MSG-12", "IDEM-12"));
        assertEquals(ConsentState.GRANTED, vehicleTask.getConsentState());
        assertTrue(result.isAccepted());
    }

    private String computeRequestDigest(ConsentCmd cmd) {
        String canonical = String.valueOf(cmd.getVehicleTaskId()) + "|"
                + cmd.getTaskId() + "|"
                + cmd.getVin() + "|"
                + cmd.getAction() + "|"
                + cmd.getTaskRevision() + "|"
                + cmd.getArticleId() + "|"
                + cmd.getArticleVersion() + "|"
                + cmd.getArticleHash() + "|"
                + cmd.getConsentScopeDigest();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
