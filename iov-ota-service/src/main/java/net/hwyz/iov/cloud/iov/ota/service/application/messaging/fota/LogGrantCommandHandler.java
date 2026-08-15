package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.LogAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.LogAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Log.LogGrantRequest;
import vehicle.fota.v1.Log.LogGrantResponse;

/**
 * 日志上传凭证命令处理器（CR-014 §5：vehicle.fota.v1.LogGrantRequest）
 *
 * <p>签发日志预签名上传地址（US-082）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class LogGrantCommandHandler {

    private final LogAppService logAppService;

    public LogGrantResponse handle(FotaMessageMetadata md, LogGrantRequest req) {
        LogAuthCmd cmd = new LogAuthCmd();
        cmd.setVin(md.vin());
        cmd.setVehicleTaskId(parseLong(md.vehicleTaskId()));
        cmd.setLogScope(String.join(",", req.getCollectionScopeList()));
        cmd.setDesensitizeVersion(req.getRedactionProfileVersion());

        LogAuthResult result = logAppService.authorizeLog(cmd);

        LogGrantResponse.Builder b = LogGrantResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setObjectKey(result.getObjectKey() == null ? "" : result.getObjectKey())
                .setMaxSizeBytes(0L);
        if (result.getCredentialToken() != null) {
            b.setUploadUrl(result.getCredentialToken());
        }
        if (result.getExpiresAt() != null) {
            b.setExpiresAtMs(result.getExpiresAt().toEpochMilli());
        }
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
