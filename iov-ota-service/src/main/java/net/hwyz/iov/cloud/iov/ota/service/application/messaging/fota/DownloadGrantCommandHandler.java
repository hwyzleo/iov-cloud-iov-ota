package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DownloadAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DownloadAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.PackageDeliveryAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Package.DownloadGrantRequest;
import vehicle.fota.v1.Package.DownloadGrantResponse;

/**
 * 下载授权命令处理器（CR-014 §5：vehicle.fota.v1.DownloadGrantRequest）
 *
 * <p>下载凭证签发/续传/重置偏移（US-078）。current_offset_bytes&gt;0 时 current_etag 必填且
 * offset_scope=STORED_OBJECT；ETag/packageRevision 变化必须 RESET_OFFSET。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class DownloadGrantCommandHandler {

    private final PackageDeliveryAppService packageDeliveryAppService;

    public DownloadGrantResponse handle(FotaMessageMetadata md, DownloadGrantRequest req) {
        DownloadAuthCmd cmd = new DownloadAuthCmd();
        cmd.setVin(md.vin());
        cmd.setVehicleTaskId(parseLong(md.vehicleTaskId()));
        cmd.setPackageId(req.getPackageId());
        cmd.setPackageRevision(req.getPackageRevision());
        if (req.hasCurrentEtag()) {
            cmd.setEtag(req.getCurrentEtag());
        }
        cmd.setOffset(req.hasCurrentOffsetBytes() ? req.getCurrentOffsetBytes() : 0L);
        cmd.setOperation(req.hasCurrentOffsetBytes() && req.getCurrentOffsetBytes() > 0 ? "RESUME" : "DOWNLOAD");

        DownloadAuthResult result = packageDeliveryAppService.authorizeDownload(cmd);

        DownloadGrantResponse.Builder b = DownloadGrantResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setPackageRevision(result.getPackageRevision() == null ? req.getPackageRevision() : result.getPackageRevision());
        if (result.isResetOffset()) {
            b.setResumeAllowed(false).setNextAction("RESET_OFFSET");
        } else {
            b.setResumeAllowed(result.getOffset() > 0).setNextAction("DOWNLOAD");
        }
        if (result.getPresignedUrl() != null) {
            b.setDownloadUrl(result.getPresignedUrl());
        }
        if (result.getExpiresAt() != null) {
            b.setExpiresAtMs(result.getExpiresAt().toEpochMilli());
        }
        b.setSupportsRange(true);
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
