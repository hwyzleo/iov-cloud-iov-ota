package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.LogObjectOperationException;
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
        // CR-026 §7.3：对象键是事实锚点，logRequestId 由 OTA 侧按对象键/幂等键关联
        cmd.setLogRequestId(md.idempotencyKey());
        cmd.setObjectKey(req.getObjectKey());
        cmd.setLogDigest(req.hasActualFileDigest() ? req.getActualFileDigest().getValueHex() : null);
        cmd.setDigestAlgorithm(req.hasActualFileDigest() ? req.getActualFileDigest().getAlgorithm() : null);
        cmd.setEtag(req.hasEtag() ? req.getEtag() : null);
        cmd.setUploadedAtMs(req.hasUploadedAtMs() && req.getUploadedAtMs() > 0 ? req.getUploadedAtMs() : null);
        cmd.setUploadResult(req.getUploadResult() == Result.RESULT_SUCCEEDED ? "SUCCESS" : "FAIL");
        cmd.setErrorCode(req.hasErrorCode() ? req.getErrorCode() : null);

        try {
            logAppService.submitLogResult(cmd);
        } catch (LogObjectOperationException exception) {
            if (exception.isRecoverable()) {
                throw exception;
            }
            // 确定性失败：回执稳定错误码，保持可表达且不无限重投
            return LogResultResponse.newBuilder()
                    .setStatus(FotaProtocols.error(String.valueOf(exception.getCode()), exception.getMessage()))
                    .setAccepted(false)
                    .build();
        }

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
