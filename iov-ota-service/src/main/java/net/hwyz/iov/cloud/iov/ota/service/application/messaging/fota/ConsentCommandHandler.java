package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ConsentCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ConsentAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Consent.ConsentReport;
import vehicle.fota.v1.Consent.ConsentResponse;
import vehicle.fota.v1.Types.ConsentStatus;

import java.time.Instant;

/**
 * 用户授权命令处理器（CR-014 §5 / CR-016 §4.20.3：vehicle.fota.v1.ConsentReport）
 *
 * <p>授权/撤回闭环（US-077、US-102～105）。accepted 仅表示上报被正常校验和记录；
 * 业务结果以 effective_consent_status 为准。同 messageId/idempotencyKey 重放返回原响应，
 * 同键异参返回 OTA-IDEMPOTENCY-CONFLICT。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class ConsentCommandHandler {

    private final ConsentAppService consentAppService;

    public ConsentResponse handle(FotaMessageMetadata md, ConsentReport req) {
        ConsentCmd cmd = new ConsentCmd();
        cmd.setVin(md.vin());
        cmd.setVehicleTaskId(parseLong(md.vehicleTaskId()));
        cmd.setAction(mapAction(req.getConsentStatus()));
        cmd.setArticleId(parseLong(req.getTermsId()));
        if (req.getTermsVersion() != null && !req.getTermsVersion().isBlank()) {
            cmd.setArticleVersion(req.getTermsVersion());
        }
        if (req.hasTermsDigest()) {
            cmd.setArticleHash(req.getTermsDigest().getValueHex());
        }
        if (req.hasConsentReceiptId()) {
            cmd.setConsentReceiptId(req.getConsentReceiptId());
        }
        if (req.getChannel() != null && !req.getChannel().isBlank()) {
            cmd.setChannel(req.getChannel());
        }
        if (req.getConsentTimeMs() > 0) {
            cmd.setReportedAt(Instant.ofEpochMilli(req.getConsentTimeMs()));
        }
        cmd.setMessageId(md.messageId());
        cmd.setIdempotencyKey(md.idempotencyKey());

        ConsentResult result = consentAppService.handleConsent(cmd);

        // 业务冲突：同键异参等
        if (result.getErrorCode() != null) {
            return ConsentResponse.newBuilder()
                    .setStatus(FotaProtocols.error(result.getErrorCode(), result.getErrorMessage()))
                    .setAccepted(false)
                    .setNextAction("NONE")
                    .build();
        }

        ConsentResponse.Builder b = ConsentResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setAccepted(result.isAccepted())
                .setEffectiveConsentStatus(FotaProtocols.consentStatus(mapEffectiveToAction(result.getEffectiveConsentState())))
                .setNextAction("DONE");
        if (result.getConsentReceiptId() != null) {
            b.setConsentReceiptId(result.getConsentReceiptId());
        }
        return b.build();
    }

    private static String mapAction(ConsentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CONSENT_STATUS_ACCEPTED -> "GRANT";
            case CONSENT_STATUS_REJECTED -> "DENY";
            case CONSENT_STATUS_REVOKED -> "REVOKE";
            default -> null;
        };
    }

    private static String mapEffectiveToAction(String state) {
        if (state == null) {
            return null;
        }
        return switch (state) {
            case "GRANTED", "ACCEPTED" -> "GRANT";
            case "REJECTED", "DENIED" -> "DENY";
            case "REVOKED" -> "REVOKE";
            default -> state;
        };
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
