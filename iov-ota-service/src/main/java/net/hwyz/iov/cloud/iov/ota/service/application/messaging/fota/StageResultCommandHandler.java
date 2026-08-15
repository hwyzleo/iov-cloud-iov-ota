package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.StageResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.PackageDeliveryAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Package.StageResultReport;
import vehicle.fota.v1.Package.StageResultResponse;
import vehicle.fota.v1.Types.Result;

/**
 * 包阶段结果命令处理器（CR-014 §5：vehicle.fota.v1.StageResultReport）
 *
 * <p>下载/验签/解密阶段终态幂等落库（US-078）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class StageResultCommandHandler {

    private final PackageDeliveryAppService packageDeliveryAppService;

    public StageResultResponse handle(FotaMessageMetadata md, StageResultReport req) {
        StageResultCmd cmd = new StageResultCmd();
        cmd.setVin(md.vin());
        cmd.setVehicleTaskId(parseLong(md.vehicleTaskId()));
        cmd.setStageResultId(req.getStageResultId());
        cmd.setPackageId(req.getPackageId());
        cmd.setStage(inferStage(req));
        cmd.setResultStatus(req.getResult() == Result.RESULT_SUCCEEDED ? "SUCCESS" : "FAILED");
        cmd.setPackageRevision(req.getVerifiedPackageRevision());
        cmd.setDigest(req.hasActualPackageDigest() ? req.getActualPackageDigest().getValueHex() : null);
        cmd.setSignatureResult(req.getSignatureVerified() ? "VERIFIED" : (req.getResult() == Result.RESULT_FAILED ? "FAILED" : "PENDING"));
        cmd.setDecryptResult(req.hasDecryptionSucceeded() ? (req.getDecryptionSucceeded() ? "SUCCESS" : "FAILED") : null);
        cmd.setFailReason(req.hasErrorCode() ? req.getErrorCode() : null);

        packageDeliveryAppService.submitStageResult(cmd);

        return StageResultResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setAccepted(true)
                .setNextAction("DONE")
                .build();
    }

    private static String inferStage(StageResultReport req) {
        if (req.hasDecryptionSucceeded()) {
            return "DECRYPT";
        }
        if (req.getHashVerified() || req.getSignatureVerified()) {
            return "VERIFY";
        }
        return "DOWNLOAD";
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
