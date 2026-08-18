package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ConsentCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ConsentAppService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Consent.ConsentReport;
import vehicle.fota.v1.Consent.ConsentResponse;
import vehicle.fota.v1.Types.ConsentStatus;
import vehicle.fota.v1.Types.Digest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ConsentCommandHandler 映射单测（CR-014 §5 / CR-016 §5：ConsentReport → ConsentCmd → ConsentResponse）
 *
 * @author hwyz_leo
 */
@DisplayName("ConsentCommandHandler - proto → 命令 → 响应映射")
class ConsentCommandHandlerTest {

    private final ConsentAppService appService = mock(ConsentAppService.class);
    private final ConsentCommandHandler handler = new ConsentCommandHandler(appService);
    private final FotaMessageMetadata md = new FotaMessageMetadata(
            "req-1", 1000L, "fota-v1", "dev-1", "LSVAU2188N2ZG4G",
            "1001", null, "idem-1", "vehicle.fota.v1.ConsentReport",
            "msg-1", null, MessageKind.MESSAGE_KIND_REQUEST, null, null);

    @Test
    @DisplayName("ACCEPTED + 条款/渠道/时间 -> ConsentCmd 映射正确 -> ACCEPTED 响应")
    void granted_maps_and_responds() {
        ConsentReport req = ConsentReport.newBuilder()
                .setConsentStatus(ConsentStatus.CONSENT_STATUS_ACCEPTED)
                .setTermsId("200").setTermsVersion("v1")
                .setTermsDigest(Digest.newBuilder().setAlgorithm("sha256").setValueHex("aabb").build())
                .setConsentTimeMs(2000L)
                .setChannel("TBOX")
                .setConsentReceiptId("RCPT-1")
                .build();
        when(appService.handleConsent(any())).thenReturn(ConsentResult.builder()
                .accepted(true).effectiveConsentState("GRANTED")
                .consentReceiptId("RCPT-1").consentState("GRANTED").build());

        ConsentResponse resp = handler.handle(md, req);

        assertEquals("0", resp.getStatus().getCode());
        assertTrue(resp.getAccepted());
        assertEquals(ConsentStatus.CONSENT_STATUS_ACCEPTED, resp.getEffectiveConsentStatus());
        assertEquals("RCPT-1", resp.getConsentReceiptId());

        ConsentCmd cmd = argumentCaptor();
        assertEquals("LSVAU2188N2ZG4G", cmd.getVin());
        assertEquals(1001L, cmd.getVehicleTaskId());
        assertEquals("GRANT", cmd.getAction());
        assertEquals(200L, cmd.getArticleId());
        assertEquals("v1", cmd.getArticleVersion());
        assertEquals("aabb", cmd.getArticleHash());
        assertEquals("TBOX", cmd.getChannel());
        assertEquals("msg-1", cmd.getMessageId());
        assertEquals("idem-1", cmd.getIdempotencyKey());
        assertEquals(2000L, cmd.getReportedAt().toEpochMilli());
    }

    @Test
    @DisplayName("REJECTED -> 映射为 DENY -> REJECTED 响应")
    void rejected_mapsToDeny() {
        ConsentReport req = ConsentReport.newBuilder()
                .setConsentStatus(ConsentStatus.CONSENT_STATUS_REJECTED)
                .setTermsId("200").setTermsDigest(Digest.newBuilder().setValueHex("aabb").build())
                .build();
        when(appService.handleConsent(any())).thenReturn(ConsentResult.builder()
                .accepted(false).effectiveConsentState("REJECTED").consentState("REJECTED").build());

        ConsentResponse resp = handler.handle(md, req);

        ConsentCmd cmd = argumentCaptor();
        assertEquals("DENY", cmd.getAction());
        assertEquals(ConsentStatus.CONSENT_STATUS_REJECTED, resp.getEffectiveConsentStatus());
        assertFalse(resp.getAccepted());
    }

    @Test
    @DisplayName("业务冲突（同键异参）-> OTA-IDEMPOTENCY-CONFLICT 错误响应")
    void conflict_returnsErrorResponse() {
        ConsentReport req = ConsentReport.newBuilder()
                .setConsentStatus(ConsentStatus.CONSENT_STATUS_ACCEPTED)
                .setTermsId("200")
                .build();
        when(appService.handleConsent(any())).thenReturn(ConsentResult.builder()
                .accepted(false)
                .errorCode(ConsentAppService.ERROR_IDEMPOTENCY_CONFLICT)
                .errorMessage("同 messageId 不同请求摘要").build());

        ConsentResponse resp = handler.handle(md, req);

        assertEquals(ConsentAppService.ERROR_IDEMPOTENCY_CONFLICT, resp.getStatus().getCode());
        assertFalse(resp.getAccepted());
        assertEquals("NONE", resp.getNextAction());
    }

    private ConsentCmd argumentCaptor() {
        var captor = org.mockito.ArgumentCaptor.forClass(ConsentCmd.class);
        verify(appService).handleConsent(captor.capture());
        return captor.getValue();
    }
}
