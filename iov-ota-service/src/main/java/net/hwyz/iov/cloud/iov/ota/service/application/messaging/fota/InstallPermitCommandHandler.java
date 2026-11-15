package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionCreateResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionAppService;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.ExecutionStateException;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Execution.InstallPermitRequest;
import vehicle.fota.v1.Execution.InstallPermitResponse;

import java.time.Instant;

/**
 * 安装许可命令处理器（CR-014 §5：vehicle.fota.v1.InstallPermitRequest）
 *
 * <p>申请安装并创建 Execution（US-079）。valid_until 只限制进入 INSTALL_STARTED。
 *
 * <p><b>拒绝语义（协议加固）</b>：安装许可的前置守卫（时间窗、活动执行、授权、阶段结果、
 * 包清单摘要等）判定失败属于**确定性业务拒绝**，必须用
 * {@code allowed=false + deny_reason} 的 RESPONSE 明确回答车端，而不得以异常形式上抛。
 * 异常上抛会被消费方当作"可恢复瞬时故障"，导致同一 offset 反复重投、分区阻壵，退避耗尽后
 * 落盘隔离，车端却始终拿不到任何答复（实车 2026-11-15：任务 3 不匹配摘要无限重投）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstallPermitCommandHandler {

    /** 业务拒绝错误码：前置守卫不满足 */
    private static final String CODE_PERMIT_DENIED = "PERMIT_DENIED";
    /** 业务拒绝错误码：请求自身不合法（如缺少 vehicle_task_id） */
    private static final String CODE_INVALID_REQUEST = "INVALID_REQUEST";
    /** 业务拒绝后的建议重试间隔（确定性拒绝不要求车端立即重试，避免高频自旋） */
    private static final long RETRY_AFTER_MS = 60_000L;

    private final ExecutionAppService executionAppService;

    public InstallPermitResponse handle(FotaMessageMetadata md, InstallPermitRequest req) {
        Long vehicleTaskId = parseLong(md.vehicleTaskId());
        if (vehicleTaskId == null) {
            log.warn("安装许可请求缺少有效 vehicle_task_id：vin[{}] messageId[{}]",
                    md.vin(), md.messageId());
            return deny(CODE_INVALID_REQUEST, "缺少有效的 vehicle_task_id，无法申请安装许可");
        }

        ExecutionCreateCmd cmd = new ExecutionCreateCmd();
        cmd.setVin(md.vin());
        cmd.setVehicleTaskId(vehicleTaskId);
        cmd.setIdempotencyKey(md.idempotencyKey());
        cmd.setInstallPlanVersion(req.getInstallPlanVersion());
        if (req.hasPackageManifestDigest()) {
            cmd.setPackageManifestDigest(req.getPackageManifestDigest().getValueHex());
            cmd.setExpectedPackageManifestDigest(req.getPackageManifestDigest().getValueHex());
        }
        cmd.setConditionSetVersion(req.getConditionSetVersion());
        // offline/timeout 策略在 InstallPermitResponse 中冻结下发，不在请求中读取

        ExecutionCreateResult result;
        try {
            result = executionAppService.requestInstall(cmd);
        } catch (ExecutionStateException e) {
            // 领域守卫失败是确定性结果，转为协议可表达的业务拒绝：车端可读、offset 可提交
            log.warn("安装许可被拒绝：vin[{}] vehicleTaskId[{}] 原因[{}]",
                    md.vin(), vehicleTaskId, e.getMessage());
            return deny(CODE_PERMIT_DENIED, safeMessage(e));
        }

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

    /**
     * 构造明确的业务拒绝响应。
     *
     * @param code   稳定错误码
     * @param reason 可定位描述
     */
    private static InstallPermitResponse deny(String code, String reason) {
        return InstallPermitResponse.newBuilder()
                .setStatus(FotaProtocols.error(code, reason))
                .setAllowed(false)
                .setDenyReason(reason)
                .setIssuedAtMs(Instant.now().toEpochMilli())
                .setNextRetryAtMs(Instant.now().toEpochMilli() + RETRY_AFTER_MS)
                .build();
    }

    private static String safeMessage(Throwable e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) {
            m = e.getClass().getSimpleName();
        }
        return m.length() > 200 ? m.substring(0, 200) : m;
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            long v = Long.parseLong(s.trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
