package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.ccp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.contract.FotaV2Request;
import net.hwyz.iov.cloud.iov.ota.api.contract.FotaV2Response;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.*;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.*;
import net.hwyz.iov.cloud.iov.ota.service.application.service.*;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.security.FotaV2ProtocolService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.security.IdempotencyService;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

/**
 * CCP FOTA v2 控制器（CR-012 §6）
 *
 * <p>统一使用 /ccp/fota/v2，旧接口保留在 v1 兼容层。
 * 所有请求先经 {@link FotaV2ProtocolService} 公共协议校验（US-073）。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/ccp/fota/v2")
public class CcpFotaV2Controller {

    private final FotaV2ProtocolService protocolService;
    private final IdempotencyService idempotencyService;

    private final TaskDetectionAppService taskDetectionAppService;
    private final ConsentAppService consentAppService;
    private final PackageDeliveryAppService packageDeliveryAppService;
    private final ExecutionAppService executionAppService;
    private final ExecutionEventAppService executionEventAppService;
    private final RecoveryAppService recoveryAppService;
    private final LogAppService logAppService;
    private final PolicySyncAppService policySyncAppService;

    // ==================== 1. 任务检测（US-074） ====================

    @PostMapping("/task/detect")
    public FotaV2Response<DetectionResult> detect(@RequestBody FotaV2Request<DetectionCmd> request) {
        return handle(request, false, "DETECT",
                () -> taskDetectionAppService.detect(request.getData()));
    }

    // ==================== 2. 本地任务/缓存处置结果 ====================

    @PostMapping("/task/{vehicleTaskId}/disposition-result")
    public FotaV2Response<Void> dispositionResult(@PathVariable Long vehicleTaskId,
                                                  @RequestBody FotaV2Request<DispositionResultCmd> request) {
        return handle(request, false, "DISPOSITION_RESULT", () -> {
            // TODO: 本地任务/缓存处置结果受理（US-076）
            log.info("车辆[{}]上报本地处置结果，车辆任务[{}]", request.getVin(), vehicleTaskId);
            return null;
        });
    }

    // ==================== 3. 授权（US-077） ====================

    @PostMapping("/task/{vehicleTaskId}/consent")
    public FotaV2Response<ConsentResult> consent(@PathVariable Long vehicleTaskId,
                                                 @RequestBody FotaV2Request<ConsentCmd> request) {
        return handle(request, true, "CONSENT",
                () -> consentAppService.handleConsent(request.getData()));
    }

    // ==================== 4. 下载授权（US-078） ====================

    @PostMapping("/task/{vehicleTaskId}/package/{packageId}/download-authorization")
    public FotaV2Response<DownloadAuthResult> downloadAuthorization(
            @PathVariable Long vehicleTaskId,
            @PathVariable String packageId,
            @RequestBody FotaV2Request<DownloadAuthCmd> request) {
        return handle(request, false, "DOWNLOAD_AUTH",
                () -> packageDeliveryAppService.authorizeDownload(request.getData()));
    }

    // ==================== 5. 包阶段结果（US-078） ====================

    @PostMapping("/task/{vehicleTaskId}/package/{packageId}/stage-result")
    public FotaV2Response<Void> stageResult(@PathVariable Long vehicleTaskId,
                                            @PathVariable String packageId,
                                            @RequestBody FotaV2Request<StageResultCmd> request) {
        return handle(request, true, "STAGE_RESULT", () -> {
            packageDeliveryAppService.submitStageResult(request.getData());
            return null;
        });
    }

    // ==================== 6. 申请安装创建 Execution（US-079） ====================

    @PostMapping("/task/{vehicleTaskId}/execution")
    public FotaV2Response<ExecutionCreateResult> createExecution(
            @PathVariable Long vehicleTaskId,
            @RequestBody FotaV2Request<ExecutionCreateCmd> request) {
        return handle(request, true, "INSTALL_EXECUTION",
                () -> executionAppService.requestInstall(request.getData()));
    }

    // ==================== 7. 安装过程事件（US-080） ====================

    @PostMapping("/execution/{executionId}/events")
    public FotaV2Response<ExecutionEventResult> events(
            @PathVariable Long executionId,
            @RequestBody FotaV2Request<ExecutionEventCmd> request) {
        return handle(request, true, "EXECUTION_EVENT",
                () -> executionEventAppService.receiveEvent(request.getData()));
    }

    // ==================== 8. 独立控制回执（US-080） ====================

    @PostMapping("/execution/{executionId}/control-acks")
    public FotaV2Response<Void> controlAcks(@PathVariable Long executionId,
                                            @RequestBody FotaV2Request<ControlAckCmd> request) {
        return handle(request, true, "CONTROL_ACK", () -> {
            executionEventAppService.receiveControlAck(request.getData());
            return null;
        });
    }

    // ==================== 9. 最终结果与收口（US-081） ====================

    @PostMapping("/execution/{executionId}/result")
    public FotaV2Response<ExecutionFinalizeResult> finalizeResult(
            @PathVariable Long executionId,
            @RequestBody FotaV2Request<ExecutionFinalizeCmd> request) {
        return handle(request, true, "EXECUTION_FINALIZE",
                () -> executionAppService.finalizeExecution(request.getData()));
    }

    // ==================== 10. 日志上传凭证（US-082） ====================

    @PostMapping("/task/{vehicleTaskId}/logs/authorization")
    public FotaV2Response<LogAuthResult> logAuthorization(
            @PathVariable Long vehicleTaskId,
            @RequestBody FotaV2Request<LogAuthCmd> request) {
        return handle(request, false, "LOG_AUTH",
                () -> logAppService.authorizeLog(request.getData()));
    }

    // ==================== 11. 日志上传结果（US-082） ====================

    @PostMapping("/task/{vehicleTaskId}/logs/result")
    public FotaV2Response<Void> logResult(@PathVariable Long vehicleTaskId,
                                          @RequestBody FotaV2Request<LogResultCmd> request) {
        return handle(request, true, "LOG_RESULT", () -> {
            logAppService.submitLogResult(request.getData());
            return null;
        });
    }

    // ==================== 12. 对账恢复（US-083） ====================

    @PostMapping("/recovery/query")
    public FotaV2Response<RecoveryResult> recoveryQuery(@RequestBody FotaV2Request<RecoveryQueryCmd> request) {
        return handle(request, false, "RECOVERY_QUERY",
                () -> recoveryAppService.query(request.getData()));
    }

    // ==================== 13. 策略同步（US-084） ====================

    @PostMapping("/policy/sync")
    public FotaV2Response<PolicySyncResult> policySync(@RequestBody FotaV2Request<PolicySyncCmd> request) {
        return handle(request, false, "POLICY_SYNC",
                () -> policySyncAppService.sync(request.getData()));
    }

    // ==================== 公共协议拦截链 ====================

    /**
     * 统一拦截链（CR-012 §4）：设备绑定、防重放、时钟偏差、协议版本、幂等键。
     */
    private <T, R> FotaV2Response<R> handle(FotaV2Request<T> request, boolean writeOperation,
                                            String operationScope, Supplier<R> executor) {
        protocolService.validateProtocolVersion(request.getProtocolVersion());
        protocolService.validateDeviceBinding(request.getVin(), request.getDeviceId());
        protocolService.validateReplayProtection(request.getTimestamp(), request.getNonce());
        protocolService.validateIdempotencyKey(request.getIdempotencyKey(), writeOperation);

        R result = idempotencyService.execute(operationScope, request.getIdempotencyKey(),
                String.valueOf(request.hashCode()), request.getVin(), executor);

        return FotaV2Response.ok(result);
    }
}
