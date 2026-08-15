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

/**
 * 用户授权命令处理器（CR-014 §5：vehicle.fota.v1.ConsentReport）
 *
 * <p>授权/撤回闭环（US-077）。accepted 仅表示上报被正常校验和记录；业务结果以
 * effective_consent_status 为准。
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
        cmd.setTermsId(parseLong(req.getTermsId()));
        if (req.hasTermsDigest()) {
            cmd.setTermsHash(req.getTermsDigest().getValueHex());
        }
        if (req.hasConsentReceiptId()) {
            cmd.setConsentReceiptId(req.getConsentReceiptId());
        }

        ConsentResult result = consentAppService.handleConsent(cmd);

        ConsentResponse.Builder b = ConsentResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setAccepted(result.isAccepted())
                .setEffectiveConsentStatus(FotaProtocols.consentStatus(mapEffective(result.getEffectiveConsentState())))
                .setNextAction("DONE");
        if (result.getConsentReceiptId() != null) {
            b.setConsentReceiptId(result.getConsentReceiptId());
        }
        if (result.isReconsentRequired()) {
            b.setConsentScopeDigest(vehicle.fota.v1.Types.Digest.newBuilder()
                    .setAlgorithm("sha256").setValueHex(result.getConsentReceiptId() == null ? "" : result.getConsentReceiptId()).build());
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

    private static String mapEffective(String state) {
        if (state == null) {
            return null;
        }
        return switch (state) {
            case "GRANTED", "ACCEPTED" -> "ACCEPTED";
            case "DENIED", "REJECTED" -> "REJECTED";
            case "REVOKED" -> "REVOKED";
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
