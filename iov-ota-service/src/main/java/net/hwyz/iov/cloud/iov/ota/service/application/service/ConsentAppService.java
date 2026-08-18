package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.api.vo.enums.VehicleTaskStatus;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ConsentCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.domain.exception.VehicleTaskStateException;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.aggregate.VehicleTask;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.entity.VehicleTaskConsent;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject.VehicleTaskId;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskConsentRepository;
import net.hwyz.iov.cloud.iov.ota.service.domain.repository.VehicleTaskRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.VehicleTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * 授权应用服务（CR-016 §4.20.3、US-102～105）
 *
 * <p>车云授权命令处理唯一入口：同一数据库事务内锁定 VehicleTask，
 * 校验归属/生命周期/taskRevision/条款 hash，按 messageId/idempotencyKey 幂等判重，
 * 追加不可变授权历史，推进当前授权状态并写响应 Outbox。
 * 投递 ACK、GatewayDeliveryStatus 与 MQTT PUBACK 不进入本服务。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentAppService {

    /** 同键异参冲突错误码（CR-016 §3.3/§5） */
    public static final String ERROR_IDEMPOTENCY_CONFLICT = "OTA-IDEMPOTENCY-CONFLICT";

    private final VehicleTaskRepository vehicleTaskRepository;
    private final VehicleTaskConsentRepository vehicleTaskConsentRepository;
    private final VehicleTaskMapper vehicleTaskMapper;

    /**
     * 处理用户授权。
     *
     * @param cmd 授权命令
     * @return 授权结果（含幂等重放与冲突语义）
     */
    @Transactional
    public ConsentResult handleConsent(ConsentCmd cmd) {
        Instant now = Instant.now();
        log.info("车辆[{}]授权，车辆任务[{}]，动作[{}]，消息[{}]",
                cmd.getVin(), cmd.getVehicleTaskId(), cmd.getAction(), cmd.getMessageId());

        // 1. 加载 VehicleTask（乐观锁基线）
        VehicleTask vt = vehicleTaskRepository.getById(VehicleTaskId.of(cmd.getVehicleTaskId()))
                .orElseThrow(() -> new VehicleTaskStateException("车辆任务[" + cmd.getVehicleTaskId() + "]不存在"));
        long expectedRowVersion = vt.getRowVersion();

        // 2. 归属校验：VIN / taskId 与消息一致
        if (!vt.getVin().equals(cmd.getVin())) {
            throw new VehicleTaskStateException("车辆任务[" + vt.getId().getValue() + "]归属校验失败：不属于车辆[" + cmd.getVin() + "]");
        }
        if (cmd.getTaskId() != null && !vt.getTaskId().equals(cmd.getTaskId())) {
            throw new VehicleTaskStateException("车辆任务[" + vt.getId().getValue() + "]归属校验失败：不属于任务[" + cmd.getTaskId() + "]");
        }

        // 3. 生命周期校验：未取消、未取代且处于允许接收授权的生命周期
        if (vt.isTerminal()) {
            throw new VehicleTaskStateException("车辆任务[" + vt.getId().getValue() + "]已处于终态[" + vt.getStatus() + "]，不可接收授权");
        }
        if (vt.getStatus() == VehicleTaskStatus.CANCELED || vt.getStatus() == VehicleTaskStatus.SUPERSEDED) {
            throw new VehicleTaskStateException("车辆任务[" + vt.getId().getValue() + "]已取消或取代，不可接收授权");
        }

        // 4. 任务修订校验：必须等于当前冻结修订
        if (cmd.getTaskRevision() != null && cmd.getTaskRevision() != vt.getTaskRevision().getValue()) {
            throw new VehicleTaskStateException("授权消息任务修订[" + cmd.getTaskRevision()
                    + "]与当前修订[" + vt.getTaskRevision().getValue() + "]不一致");
        }

        // 5. 条款校验：需授权时，冻结条款 hash 必须一致
        if (vt.isConsentRequired()) {
            if (vt.getConsentArticleHash() == null || !vt.getConsentArticleHash().equals(cmd.getArticleHash())) {
                throw new VehicleTaskStateException("授权条款摘要与发布冻结条款不一致");
            }
        }

        // 6. 请求摘要（幂等冲突检测依据）
        String requestDigest = computeRequestDigest(cmd);

        // 7. 幂等判重：同 messageId/idempotencyKey 同摘要复用原响应；同键异参冲突
        ConsentResult dedup = checkIdempotency(cmd, requestDigest);
        if (dedup != null) {
            return dedup;
        }

        // 8. 业务结果与范围摘要
        net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult domainResult = mapAction(cmd.getAction());
        String scopeDigest = computeScopeDigest(vt, cmd);

        // 9. GRANTED 生成稳定回执
        String receiptId = domainResult == net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult.GRANTED
                ? (cmd.getConsentReceiptId() != null && !cmd.getConsentReceiptId().isBlank()
                    ? cmd.getConsentReceiptId()
                    : "consent-" + UUID.randomUUID().toString().replace("-", ""))
                : cmd.getConsentReceiptId();

        // 10. 追加不可变授权历史（同一事务）
        VehicleTaskConsent record = new VehicleTaskConsent()
                .setVehicleTaskId(vt.getId().getValue())
                .setTaskId(vt.getTaskId())
                .setVin(vt.getVin())
                .setTaskRevision(vt.getTaskRevision().getValue())
                .setResult(domainResult)
                .setConsentReceiptId(receiptId)
                .setSupersedesConsentId(vt.getCurrentConsentId())
                .setArticleId(vt.getConsentArticleId())
                .setArticleVersion(vt.getConsentArticleVersion())
                .setArticleHash(vt.getConsentArticleHash())
                .setConsentScopeDigest(scopeDigest)
                .setChannel(cmd.getChannel())
                .setSubjectRef(cmd.getSubjectRef())
                .setReportedAt(cmd.getReportedAt())
                .setReceivedAt(now)
                .setExpireAt(cmd.getExpireAt())
                .setMessageId(cmd.getMessageId())
                .setIdempotencyKey(normalizeKey(cmd.getIdempotencyKey()))
                .setRequestDigest(requestDigest)
                .setSourceModel("NATIVE");
        record = vehicleTaskConsentRepository.append(record);

        // 11. 推进当前授权状态（乐观锁 row_version 校验）
        boolean downloadRequired = cmd.getDownloadRequired() != null ? cmd.getDownloadRequired() : true;
        vt.applyConsent(domainResult, record.getId(), scopeDigest, now, downloadRequired);
        int updated = vehicleTaskMapper.updateCurrentConsent(
                vt.getId().getValue(),
                expectedRowVersion,
                vt.getStatus() != null ? vt.getStatus().getValue() : null,
                vt.getDownloadReadyState() != null ? vt.getDownloadReadyState().getValue() : null,
                vt.getConsentState() != null ? vt.getConsentState().getValue() : null,
                vt.getCurrentConsentId(),
                vt.getConsentScopeDigest(),
                Date.from(vt.getConsentUpdatedAt()));
        if (updated != 1) {
            throw new VehicleTaskStateException("车辆任务[" + vt.getId().getValue() + "]授权状态并发冲突，请重试");
        }
        vt.setRowVersion(expectedRowVersion + 1);

        // 12. 业务响应 Outbox（由 CommandHandler 构建后经 PayloadHandler 追加，同事务）
        return ConsentResult.builder()
                .consentReceiptId(receiptId)
                .consentState(domainResult.getValue())
                .accepted(domainResult == net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult.GRANTED)
                .effectiveConsentState(vt.getConsentState().getValue())
                .consentRecordId(record.getId())
                .currentConsentState(vt.getConsentState().getValue())
                .build();
    }

    /**
     * 幂等判重：同 messageId/idempotencyKey 同摘要返回原响应；同键异参返回冲突结果。
     */
    private ConsentResult checkIdempotency(ConsentCmd cmd, String requestDigest) {
        if (cmd.getMessageId() != null && !cmd.getMessageId().isBlank()) {
            Optional<VehicleTaskConsent> existing = vehicleTaskConsentRepository.findByMessageId(cmd.getMessageId());
            if (existing.isPresent()) {
                return resolveDedup(existing.get(), requestDigest, "messageId[" + cmd.getMessageId() + "]");
            }
        }
        String idemKey = normalizeKey(cmd.getIdempotencyKey());
        if (idemKey != null) {
            Optional<VehicleTaskConsent> existing = vehicleTaskConsentRepository.findByIdempotencyKey(idemKey);
            if (existing.isPresent()) {
                return resolveDedup(existing.get(), requestDigest, "idempotencyKey[" + idemKey + "]");
            }
        }
        return null;
    }

    private ConsentResult resolveDedup(VehicleTaskConsent existing, String requestDigest, String keyDesc) {
        if (existing.getRequestDigest() != null && existing.getRequestDigest().equals(requestDigest)) {
            log.info("授权幂等命中：{}，复用原响应", keyDesc);
            return ConsentResult.builder()
                    .consentReceiptId(existing.getConsentReceiptId())
                    .consentState(existing.getResult() != null ? existing.getResult().getValue() : null)
                    .accepted(existing.getResult() == net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult.GRANTED)
                    .effectiveConsentState(existing.getResult() != null ? existing.getResult().getValue() : null)
                    .consentRecordId(existing.getId())
                    .currentConsentState(existing.getResult() != null ? existing.getResult().getValue() : null)
                    .replayed(true)
                    .build();
        }
        log.warn("授权同键异参冲突：{} 原摘要[{}] 新摘要[{}]", keyDesc, existing.getRequestDigest(), requestDigest);
        return ConsentResult.builder()
                .accepted(false)
                .errorCode(ERROR_IDEMPOTENCY_CONFLICT)
                .errorMessage("同 " + keyDesc + " 不同请求摘要（冲突隔离）")
                .build();
    }

    private net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult mapAction(String action) {
        if (action == null) {
            throw new VehicleTaskStateException("未知授权动作: " + action);
        }
        return switch (action) {
            case "GRANT" -> net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult.GRANTED;
            case "DENY" -> net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult.REJECTED;
            case "REVOKE" -> net.hwyz.iov.cloud.iov.ota.api.vo.enums.ConsentResult.REVOKED;
            default -> throw new VehicleTaskStateException("未知授权动作: " + action);
        };
    }

    /**
     * 授权范围摘要：绑定任务修订、冻结条款集合与受控动作范围。
     */
    private String computeScopeDigest(VehicleTask vt, ConsentCmd cmd) {
        String scope = vt.getTaskRevision().getValue() + "|"
                + vt.getConsentArticleId() + "|"
                + vt.getConsentArticleVersion() + "|"
                + vt.getConsentArticleHash() + "|"
                + cmd.getAction();
        return sha256(scope);
    }

    /**
     * 请求摘要：同幂等键异参冲突检测依据。
     */
    private String computeRequestDigest(ConsentCmd cmd) {
        String canonical = String.valueOf(cmd.getVehicleTaskId()) + "|"
                + cmd.getTaskId() + "|"
                + cmd.getVin() + "|"
                + cmd.getAction() + "|"
                + cmd.getTaskRevision() + "|"
                + cmd.getArticleId() + "|"
                + cmd.getArticleVersion() + "|"
                + cmd.getArticleHash() + "|"
                + cmd.getConsentScopeDigest();
        return sha256(canonical);
    }

    private String normalizeKey(String key) {
        return key == null || key.isBlank() ? null : key;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }
}
