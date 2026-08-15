package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.PolicySyncCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.PolicySyncResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.PolicySyncAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Policy.EffectivePolicy;
import vehicle.fota.v1.Policy.PolicyRequest;
import vehicle.fota.v1.Policy.PolicyResponse;

/**
 * 策略同步命令处理器（CR-014 §5：vehicle.fota.v1.PolicyRequest）
 *
 * <p>偏好与有效策略同步（US-084）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class PolicyCommandHandler {

    private final PolicySyncAppService policySyncAppService;

    public PolicyResponse handle(FotaMessageMetadata md, PolicyRequest req) {
        PolicySyncCmd cmd = new PolicySyncCmd();
        cmd.setVin(md.vin());
        cmd.setVehicleTaskId(parseLong(md.vehicleTaskId()));
        if (req.hasBasePreferenceVersion()) {
            cmd.setBasePreferenceVersion(parseLong(req.getBasePreferenceVersion()));
        }
        cmd.setUserPreference(req.hasUserPreference() ? FotaJson.toJson(req.getUserPreference()) : null);

        PolicySyncResult result = policySyncAppService.sync(cmd);

        PolicyResponse.Builder b = PolicyResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setEffectivePolicy(EffectivePolicy.newBuilder()
                        .setAutoDownload(false)
                        .setInstallMode("SCHEDULED")
                        .setMaxRetryAttempts(3)
                        .build())
                .setPolicyVersion(result.getPreferenceVersion() == null ? "0" : String.valueOf(result.getPreferenceVersion()))
                .setEffectiveAtMs(System.currentTimeMillis())
                .setCurrentPreferenceVersion(result.getPreferenceVersion() == null ? "0" : String.valueOf(result.getPreferenceVersion()))
                .setPreferenceAccepted(true);
        return b.build();
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
