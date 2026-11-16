package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.LogAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.LogObjectOperationException;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.LogObjectStorageGateway;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.UpgradeLog;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.UpgradeLogRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 日志应用服务（IOV-OTA-DSN-CR-026 §7.3、US-035/US-082）。
 *
 * <p>闭环：授权登记（AUTHORIZED）→ 车端真实 PUT → 结果确认 → EDD-OSS HEAD 存在性/大小/摘要
 * 核验 → 只有核验通过才置 UPLOADED。对象存储调用发生在持久化事务之外，落库使用短事务；
 * 存储暂不可用按可恢复故障抛出，确定性失败保留 FAILED 状态与稳定原因。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class LogAppService {

    /** 支持的日志 Content-Type（与 EDD-OSS 白名单一致） */
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    /** 摘要算法 */
    private static final String DIGEST_ALGORITHM = "sha-256";
    /** 默认采集范围 */
    private static final String DEFAULT_SCOPE = "ccu-fota";

    private final VehicleTaskRepository vehicleTaskRepository;
    private final UpgradeLogRepository upgradeLogRepository;
    private final LogObjectStorageGateway logObjectStorageGateway;
    private final KafkaMessagingMetricsService metrics;
    private final TransactionTemplate shortTx;

    public LogAppService(VehicleTaskRepository vehicleTaskRepository,
                         UpgradeLogRepository upgradeLogRepository,
                         LogObjectStorageGateway logObjectStorageGateway,
                         KafkaMessagingMetricsService metrics,
                         PlatformTransactionManager transactionManager) {
        this.vehicleTaskRepository = vehicleTaskRepository;
        this.upgradeLogRepository = upgradeLogRepository;
        this.logObjectStorageGateway = logObjectStorageGateway;
        this.metrics = metrics;
        this.shortTx = new TransactionTemplate(transactionManager);
        this.shortTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 申请日志上传凭证（CR-026 §7.3 authorize）。
     *
     * @param cmd 日志授权命令
     * @return 授权结果（含预签名 PUT 地址与强制头）
     */
    public LogAuthResult authorizeLog(LogAuthCmd cmd) {
        VehicleTask vehicleTask = requireCurrentVehicleTask(cmd.getVehicleTaskId());
        String sha256Hex = requireSha256(cmd.getSha256Hex());
        long sizeBytes = requirePositiveSize(cmd.getSizeBytes());
        String contentType = requireContentType(cmd.getContentType());
        String privacyLevel = requirePrivacyLevel(cmd.getPrivacyLevel());
        String logRequestId = cmd.getLogRequestId() == null || cmd.getLogRequestId().isBlank()
                ? "log-" + UUID.randomUUID().toString().replace("-", "") : cmd.getLogRequestId();

        LogObjectStorageGateway.Authorization authorization = logObjectStorageGateway.authorize(
                new LogObjectStorageGateway.AuthorizationRequest("OTA_UPGRADE_LOG",
                        String.valueOf(vehicleTask.getId().getValue()),
                        cmd.getExecutionId() == null ? null : String.valueOf(cmd.getExecutionId()),
                        logRequestId, requireFileName(cmd.getFileName()), contentType, sizeBytes, sha256Hex,
                        privacyLevel, cmd.getTtlSeconds()));

        Optional<UpgradeLog> existing = upgradeLogRepository.findByLogRequestId(logRequestId);
        if (existing.isPresent() && existing.get().getObjectKey() != null
                && !existing.get().getObjectKey().equals(authorization.objectKey())) {
            metrics.increment("log_authorize_conflict");
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "同一 logRequestId 对应不同对象键");
        }
        Instant now = Instant.now();
        UpgradeLog logEntity = existing.orElseGet(() -> UpgradeLog.builder()
                .taskId(vehicleTask.getTaskId())
                .vin(vehicleTask.getVin())
                .vehicleTaskId(vehicleTask.getId().getValue())
                .executionId(cmd.getExecutionId())
                .logRequestId(logRequestId)
                .logScope(cmd.getLogScope() == null || cmd.getLogScope().isBlank() ? DEFAULT_SCOPE : cmd.getLogScope())
                .desensitizeVersion(cmd.getDesensitizeVersion())
                .privacyLevel(privacyLevel)
                .objectKey(authorization.objectKey())
                .logObjectId(authorization.objectId())
                .objectSizeBytes(sizeBytes)
                .logDigest(sha256Hex)
                .digestAlgorithm(DIGEST_ALGORITHM)
                .uploadState(UpgradeLog.STATE_AUTHORIZED)
                .authorizedAt(now)
                .build());
        logEntity.setLogObjectId(authorization.objectId());
        logEntity.setObjectKey(authorization.objectKey());
        shortTx.executeWithoutResult(status -> upgradeLogRepository.save(logEntity));

        log.info("车辆任务[{}]日志对象已授权 logRequestId[{}] objectId[{}]", cmd.getVehicleTaskId(),
                logRequestId, authorization.objectId());
        return LogAuthResult.builder()
                .logRequestId(logRequestId)
                .objectId(authorization.objectId())
                .objectKey(authorization.objectKey())
                .uploadUrl(authorization.uploadUrl())
                .headers(authorization.headers())
                .maxSizeBytes(authorization.maxSizeBytes())
                .expiresAt(authorization.expiresAt())
                .build();
    }

    /**
     * 提交日志上传结果（CR-026 §7.3 result）。
     *
     * @param cmd 日志结果命令
     */
    public void submitLogResult(LogResultCmd cmd) {
        UpgradeLog logEntity = requireLog(cmd);
        Instant now = Instant.now();
        if (logEntity.isUploaded()) {
            log.info("日志[{}]已核验通过，幂等返回", logEntity.getLogRequestId());
            return;
        }
        if (logEntity.isFailed()) {
            throw failure(logEntity.getFailureReason());
        }
        // 对象键绑定校验先于任何存储调用
        if (cmd.getObjectKey() != null && !cmd.getObjectKey().isBlank()
                && !cmd.getObjectKey().equals(logEntity.getObjectKey())) {
            markFailed(logEntity, "OBJECT_KEY_MISMATCH", now);
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_OBJECT_MISMATCH,
                    "上报对象键与授权对象键不一致");
        }
        if (!"SUCCESS".equalsIgnoreCase(cmd.getUploadResult())) {
            // 车端明确失败：保留 FAILED 事实与原因，不调用存储
            markFailed(logEntity, cmd.getErrorCode() == null || cmd.getErrorCode().isBlank()
                    ? "UPLOAD_FAILED" : cmd.getErrorCode(), now);
            return;
        }
        if (cmd.getLogDigest() != null && !cmd.getLogDigest().isBlank()
                && logEntity.getLogDigest() != null
                && !cmd.getLogDigest().trim().toLowerCase(Locale.ROOT).equals(logEntity.getLogDigest())) {
            markFailed(logEntity, "DIGEST_MISMATCH", now);
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_OBJECT_MISMATCH,
                    "上报摘要与授权声明不一致");
        }
        // 存储核验在持久化事务之外（幂等，不持有数据库行锁）
        LogObjectStorageGateway.Fact fact;
        try {
            fact = logObjectStorageGateway.complete(logEntity.getLogObjectId());
        } catch (LogObjectOperationException exception) {
            metrics.increment("log_object_verify_failed");
            if (exception.isRecoverable()) {
                // 可恢复：不改状态、不落 UPLOADED，由 Kafka 重投按原 objectId 重试
                throw exception;
            }
            markFailed(logEntity, resolveFailureReason(exception.getCode()), now);
            throw exception;
        }
        if (!"UPLOADED".equalsIgnoreCase(fact.uploadState())) {
            metrics.increment("log_object_verify_failed");
            markFailed(logEntity, fact.failureReason() == null ? "UPLOAD_NOT_CONFIRMED" : fact.failureReason(), now);
            throw failure(fact.failureReason());
        }
        logEntity.setObjectSizeBytes(fact.actualSizeBytes() == null
                ? logEntity.getObjectSizeBytes() : fact.actualSizeBytes());
        logEntity.setLogDigest(fact.actualSha256Hex() == null ? logEntity.getLogDigest() : fact.actualSha256Hex());
        logEntity.setReceivedAt(logEntity.getReceivedAt() == null ? now : logEntity.getReceivedAt());
        logEntity.markUploaded(fact.uploadedAt() == null ? now : fact.uploadedAt());
        shortTx.executeWithoutResult(status -> upgradeLogRepository.save(logEntity));
        metrics.increment("log_object_verified");
        log.info("日志[{}]对象核验通过并登记 UPLOADED，objectKey[{}]", logEntity.getLogRequestId(),
                logEntity.getObjectKey());
    }

    /**
     * 申请短期受控下载地址（不在列表/消息中返回长期 URL）。
     *
     * @param vehicleTaskId 车辆任务ID
     * @param logRequestId  日志申请ID
     * @return 短期下载地址
     */
    public String downloadUrl(Long vehicleTaskId, String logRequestId) {
        requireCurrentVehicleTask(vehicleTaskId);
        UpgradeLog logEntity = upgradeLogRepository.findByLogRequestId(logRequestId)
                .orElseThrow(() -> new LogObjectOperationException(
                        LogObjectOperationException.ERROR_CODE_STATE_CONFLICT, "日志登记不存在"));
        if (!vehicleTaskId.equals(logEntity.getVehicleTaskId())) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "日志不属于该车辆任务");
        }
        if (!logEntity.isUploaded()) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "日志未核验通过，不可下载");
        }
        return logObjectStorageGateway.downloadUrl(logEntity.getLogObjectId()).downloadUrl();
    }

    private UpgradeLog requireLog(LogResultCmd cmd) {
        Optional<UpgradeLog> byRequest = upgradeLogRepository.findByLogRequestId(cmd.getLogRequestId());
        if (byRequest.isPresent()) {
            return byRequest.get();
        }
        Optional<UpgradeLog> byKey = upgradeLogRepository.findByObjectKey(cmd.getObjectKey());
        if (byKey.isPresent()) {
            return byKey.get();
        }
        metrics.increment("log_result_unknown");
        throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                "日志登记不存在：logRequestId[" + cmd.getLogRequestId() + "]");
    }

    private void markFailed(UpgradeLog logEntity, String reason, Instant now) {
        logEntity.setReceivedAt(logEntity.getReceivedAt() == null ? now : logEntity.getReceivedAt());
        logEntity.markFailed(reason);
        shortTx.executeWithoutResult(status -> upgradeLogRepository.save(logEntity));
    }

    private VehicleTask requireCurrentVehicleTask(Long vehicleTaskId) {
        if (vehicleTaskId == null) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "车辆任务ID缺失");
        }
        VehicleTask vehicleTask = vehicleTaskRepository.getById(VehicleTaskId.of(vehicleTaskId))
                .orElseThrow(() -> new LogObjectOperationException(
                        LogObjectOperationException.ERROR_CODE_STATE_CONFLICT, "车辆任务不存在"));
        if (vehicleTask.getSupersededBy() != null) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "车辆任务已被取代，拒绝日志操作");
        }
        return vehicleTask;
    }

    private static String requireSha256(String sha256Hex) {
        String normalized = sha256Hex == null ? null : sha256Hex.trim().toLowerCase(Locale.ROOT);
        if (normalized == null || !SHA256.matcher(normalized).matches()) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "日志摘要缺失或非法（需 64 位小写十六进制 SHA-256）");
        }
        return normalized;
    }

    private static long requirePositiveSize(Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "日志声明大小缺失或非法");
        }
        return sizeBytes;
    }

    private static String requireContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "日志 Content-Type 缺失");
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private static String requirePrivacyLevel(String privacyLevel) {
        if (privacyLevel == null || privacyLevel.isBlank()) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "隐私分级缺失");
        }
        return privacyLevel.trim();
    }

    private static String requireFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_STATE_CONFLICT,
                    "日志文件名缺失");
        }
        return fileName.trim();
    }

    private static LogObjectOperationException failure(String reason) {
        if ("OBJECT_NOT_FOUND".equals(reason) || "LOG_OBJECT_MISSING".equals(reason)) {
            return new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_OBJECT_NOT_FOUND,
                    "日志对象不存在");
        }
        return new LogObjectOperationException(LogObjectOperationException.ERROR_CODE_OBJECT_MISMATCH,
                "日志对象核验失败：" + reason);
    }

    private static String resolveFailureReason(int errorCode) {
        return switch (errorCode) {
            case LogObjectOperationException.ERROR_CODE_OBJECT_NOT_FOUND -> "OBJECT_NOT_FOUND";
            case LogObjectOperationException.ERROR_CODE_OBJECT_MISMATCH -> "OBJECT_MISMATCH";
            case LogObjectOperationException.ERROR_CODE_STATE_CONFLICT -> "STATE_CONFLICT";
            default -> "VERIFY_FAILED";
        };
    }
}
