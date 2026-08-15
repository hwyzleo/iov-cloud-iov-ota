package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.LogAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Log.LogResultResponse;
import vehicle.fota.v1.Log.LogUploadResult;
import vehicle.fota.v1.Types.Result;

/**
 * 日志上传结果命令处理器（CR-014 §5：vehicle.fota.v1.LogUploadResult）
 *
 * <p>确认日志上传结果（US-082）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class LogResultCommandHandler {

    private final LogAppService logAppService;

    public LogResultResponse handle(FotaMessageMetadata md, LogUploadResult req) {
        LogResultCmd cmd = new LogResultCmd();
        cmd.setVin(md.vin());
        cmd.setVehicleTaskId(parseLong(md.vehicleTaskId()));
        cmd.setLogRequestId(md.idempotencyKey());
        cmd.setObjectKey(req.getObjectKey());
        cmd.setLogDigest(req.hasActualFileDigest() ? req.getActualFileDigest().getValueHex() : null);
        cmd.setUploadResult(req.getUploadResult() == Result.RESULT_SUCCEEDED ? "SUCCESS" : "FAIL");

        logAppService.submitLogResult(cmd);

        return LogResultResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setAccepted(true)
                .build();
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
