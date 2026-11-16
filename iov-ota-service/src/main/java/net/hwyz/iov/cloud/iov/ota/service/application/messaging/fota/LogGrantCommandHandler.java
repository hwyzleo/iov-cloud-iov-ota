package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.LogAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.LogObjectOperationException;
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
        cmd.setExecutionId(parseLong(md.executionId()));
        cmd.setLogRequestId(req.getLogRequestId());
        cmd.setLogScope(String.join(",", req.getCollectionScopeList()));
        cmd.setDesensitizeVersion(req.getRedactionProfileVersion());
        cmd.setFileName(req.getFileName());
        cmd.setContentType(req.getMimeType());
        cmd.setSizeBytes(req.getFileSizeBytes() > 0 ? req.getFileSizeBytes() : null);
        cmd.setSha256Hex(req.hasFileDigest() ? req.getFileDigest().getValueHex() : null);
        cmd.setPrivacyLevel(req.getPrivacyLevel());

        LogAuthResult result;
        try {
            result = logAppService.authorizeLog(cmd);
        } catch (LogObjectOperationException exception) {
            if (exception.isRecoverable()) {
                throw exception;
            }
            // 确定性拒绝：以可表达的业务错误响应回执，不抛异常避免事务回滚与无限重投
            return LogGrantResponse.newBuilder()
                    .setStatus(FotaProtocols.error(String.valueOf(exception.getCode()), exception.getMessage()))
                    .build();
        }

        LogGrantResponse.Builder b = LogGrantResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setObjectKey(result.getObjectKey() == null ? "" : result.getObjectKey());
        if (result.getUploadUrl() != null) {
            b.setUploadUrl(result.getUploadUrl());
        }
        if (result.getHeaders() != null) {
            b.putAllHeaders(result.getHeaders());
        }
        if (result.getExpiresAt() != null) {
            b.setExpiresAtMs(result.getExpiresAt().toEpochMilli());
        }
        if (result.getMaxSizeBytes() != null) {
            b.setMaxSizeBytes(result.getMaxSizeBytes());
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
