package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionCreateResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Execution.InstallPermitRequest;
import vehicle.fota.v1.Execution.InstallPermitResponse;

/**
 * 安装许可命令处理器（CR-014 §5：vehicle.fota.v1.InstallPermitRequest）
 *
 * <p>申请安装并创建 Execution（US-079）。valid_until 只限制进入 INSTALL_STARTED。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class InstallPermitCommandHandler {

    private final ExecutionAppService executionAppService;

    public InstallPermitResponse handle(FotaMessageMetadata md, InstallPermitRequest req) {
        ExecutionCreateCmd cmd = new ExecutionCreateCmd();
        cmd.setVin(md.vin());
        cmd.setVehicleTaskId(parseLong(md.vehicleTaskId()));
        cmd.setIdempotencyKey(md.idempotencyKey());
        cmd.setInstallPlanVersion(req.getInstallPlanVersion());
        if (req.hasPackageManifestDigest()) {
            cmd.setPackageManifestDigest(req.getPackageManifestDigest().getValueHex());
            cmd.setExpectedPackageManifestDigest(req.getPackageManifestDigest().getValueHex());
        }
        cmd.setConditionSetVersion(req.getConditionSetVersion());
        // offline/timeout 策略在 InstallPermitResponse 中冻结下发，不在请求中读取

        ExecutionCreateResult result = executionAppService.requestInstall(cmd);

        InstallPermitResponse.Builder b = InstallPermitResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setAllowed(true)
                .setExecutionId(String.valueOf(result.getExecutionId()))
                .setAttemptNo(result.getAttemptNo())
                .setPermitId("permit-" + result.getExecutionId())
                .setPermitToken(result.getPermitToken())
                .setIssuedAtMs(System.currentTimeMillis())
                .setControlRevision(1L)
                .setInstallPlanVersion(result.getInstallPlanVersion() == null ? "" : result.getInstallPlanVersion());
        if (result.getValidUntil() != null) {
            b.setValidUntilMs(result.getValidUntil().toEpochMilli());
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
