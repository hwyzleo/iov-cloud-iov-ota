package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxPo;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox.KafkaInboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;
import vehicle.common.v1.Envelope.VehicleMessageEnvelope;

/**
 * FOTA 入站处理流水线（CR-014 §4.2）
 *
 * <pre>
 * consume(record):
 *   raw = record.value
 *   envelope = VehicleMessageEnvelope.parseFrom(raw)
 *   validate record.key == envelope.vin
 *   validate service == vehicle.fota / protocol major / required / TTL / size
 *   validate payload_type in registry / message_kind direction matrix
 *   handler = payloadRouter.resolve(payload_type)
 *   typedPayload = handler.parse(envelope.payload)
 *   inboxResult = inbox.claim(message_id, sha256(raw))
 *   duplicate same hash -> 幂等复用；duplicate different hash -> 冲突隔离
 *   invoke handler.handle(metadata, typedPayload)
 * </pre>
 *
 * <p><b>事务边界（2026-11-15 实车加固）</b>：业务执行与 Inbox 记账共用同一个事务模板（成功路径仍然
 * 原子提交），但业务异常会先让该事务**干净回滚**，再由 {@code failureTx}(REQUIRES_NEW) 在新事务里
 * 写「可表达的业务拒绝响应」+ Inbox 记账。早期实现把整个流程放在一个 {@code @Transactional} 方法里，
 * 内层 {@code @Transactional} 业务方法抛 RuntimeException 会把共享事务标记 rollback-only，
 * 外层 catch 后继续返回，提交阶段才抛 {@link UnexpectedRollbackException}：拒绝响应连同 Outbox
 * 一起回滚，车端拿不到任何答复，offset 反复重投直到退避耗尽落盘隔离（实车 2026-11-15 任务 4）。
 *
 * <p>可恢复异常抛出（事务回滚、offset 不提交、由 Kafka 重投）；
 * 不可恢复契约错误由消费方转 DLQ/隔离并生产业务拒绝事件。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class FotaKafkaInboundHandler {

    /** 业务异常回带错误响应的稳定错误码 */
    private static final String CODE_BUSINESS_ERROR = "BUSINESS_ERROR";
    /** 业务异常回带错误响应的稳定错误码：业务方法吞掉异常后仍把事务标记为 rollback-only */
    private static final String CODE_ROLLBACK_ONLY = "TRANSACTION_ROLLBACK_ONLY";

    private final FotaEnvelopeValidator validator;
    private final FotaPayloadRouter router;
    private final KafkaInboxRepository inboxRepository;
    private final KafkaMessagingMetricsService metrics;

    /** 业务执行 + Inbox 记账事务（成功路径原子提交） */
    private final TransactionTemplate businessTx;
    /** 业务异常后的拒绝响应事务（必须独立于已回滚/已被标记 rollback-only 的业务事务） */
    private final TransactionTemplate failureTx;

    public FotaKafkaInboundHandler(FotaEnvelopeValidator validator,
                                   FotaPayloadRouter router,
                                   KafkaInboxRepository inboxRepository,
                                   KafkaMessagingMetricsService metrics,
                                   PlatformTransactionManager transactionManager) {
        this.validator = validator;
        this.router = router;
        this.inboxRepository = inboxRepository;
        this.metrics = metrics;
        this.businessTx = new TransactionTemplate(transactionManager);
        this.failureTx = new TransactionTemplate(transactionManager);
        this.failureTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 处理一条 Kafka record。
     *
     * @throws OtaKafkaMessagingException 可恢复异常（回滚重投）或不可恢复异常（契约错误，由消费方转 DLQ）
     */
    public void processMessage(ConsumerRecord<String, byte[]> record) {
        byte[] raw = record.value();
        if (raw == null || raw.length == 0) {
            throw OtaKafkaMessagingException.nonRecoverable("Kafka record 值为空");
        }
        VehicleMessageEnvelope envelope;
        try {
            envelope = VehicleMessageEnvelope.parseFrom(raw);
        } catch (InvalidProtocolBufferException e) {
            throw OtaKafkaMessagingException.nonRecoverable("Envelope 解析失败: " + e.getMessage());
        }
        validator.validate(record, envelope);

        String payloadType = envelope.getPayloadType();
        @SuppressWarnings("rawtypes")
        FotaPayloadHandler handler = router.resolve(payloadType);
        FotaMessageMetadata metadata = FotaMessageMetadata.fromEnvelope(envelope);

        Message typedPayload = parsePayload(handler, envelope);
        String sha = FotaDigests.sha256(raw);

        try {
            // 首次处理：业务执行 + 领域状态 + 结果 Outbox + Inbox 记账同事务
            businessTx.executeWithoutResult(status -> handleInTransaction(
                    record, envelope, payloadType, handler, metadata, typedPayload, sha));
        } catch (BusinessFailure failure) {
            // 业务事务已回滚：拒绝响应必须在独立事务中落库，否则会被一起回滚、车端永远收不到答复
            handleBusinessFailure(record, envelope, payloadType, handler, metadata, typedPayload, sha,
                    CODE_BUSINESS_ERROR, failure.getCause());
        } catch (UnexpectedRollbackException e) {
            // 业务方法“吞掉异常后继续返回”，但内层事务已把共享事务标记 rollback-only，提交阶段才暴露。
            // 此时业务写入同样已回滚，必须在新事务里补一条可表达的错误响应，避免 offset 无限重投。
            handleBusinessFailure(record, envelope, payloadType, handler, metadata, typedPayload, sha,
                    CODE_ROLLBACK_ONLY, e);
        }
    }

    /**
     * 在业务事务内执行：Inbox 幂等/冲突判定 + 业务处理 + Inbox 记账。
     *
     * @throws BusinessFailure 业务异常（包一层以便事务边界先回滚，再在事务外写拒绝响应）
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handleInTransaction(ConsumerRecord<String, byte[]> record,
                                     VehicleMessageEnvelope envelope,
                                     String payloadType,
                                     FotaPayloadHandler handler,
                                     FotaMessageMetadata metadata,
                                     Message typedPayload,
                                     String sha) {
        String consumerName = payloadType;
        // 同事务内锁定同 message_id（唯一索引+间隙锁串行化同键并发）
        KafkaInboxPo existing = inboxRepository.selectForUpdate(consumerName, envelope.getMessageId());
        if (existing != null) {
            metrics.increment(KafkaMessagingMetricsService.INBOX_DUPLICATE);
            if (existing.getEnvelopeSha256() != null && existing.getEnvelopeSha256().equals(sha)) {
                // 同 message_id 同摘要 → 幂等，复用原结果
                log.info("Inbox 幂等命中：payloadType[{}] messageId[{}]，跳过重复处理", payloadType, envelope.getMessageId());
                return;
            }
            // 同 message_id 异摘要 → 冲突隔离，生产业务拒绝事件
            metrics.increment(KafkaMessagingMetricsService.INBOX_DIGEST_CONFLICT);
            log.warn("Inbox Envelope 摘要冲突：payloadType[{}] messageId[{}] 原摘要[{}] 新摘要[{}]",
                    payloadType, envelope.getMessageId(), existing.getEnvelopeSha256(), sha);
            handler.handleConflict(metadata, typedPayload, "同 message_id 不同 Envelope 摘要（冲突隔离）");
            inboxRepository.markConflict(consumerName, envelope.getMessageId(), "Envelope 摘要冲突");
            return;
        }

        Long resultOutboxId;
        try {
            resultOutboxId = handler.handle(metadata, typedPayload);
        } catch (RuntimeException e) {
            // 直接抛出会让内层 @Transactional 把本事务标记 rollback-only，且拒绝响应无从写入；
            // 包成 BusinessFailure 退出事务边界，由外层在新事务里回带错误响应。
            throw new BusinessFailure(e);
        }

        inboxRepository.markProcessed(inboxPo(record, envelope, payloadType, sha, resultOutboxId));
        metrics.increment(KafkaMessagingMetricsService.INBOX_TOTAL);
        log.info("FOTA 消息处理完成：payloadType[{}] messageId[{}] offset[{}]",
                payloadType, envelope.getMessageId(), record.offset());
    }

    /**
     * 业务异常后在新事务里写「可表达的业务拒绝响应」并记账 Inbox。
     *
     * <p>handler 未提供拒绝响应（{@code handleFailure} 返回 null）时按可恢复异常上抛，保持原语义。
     *
     * @param code   稳定错误码
     * @param cause  原始业务异常
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handleBusinessFailure(ConsumerRecord<String, byte[]> record,
                                       VehicleMessageEnvelope envelope,
                                       String payloadType,
                                       FotaPayloadHandler handler,
                                       FotaMessageMetadata metadata,
                                       Message typedPayload,
                                       String sha,
                                       String code,
                                       Throwable cause) {
        Long failureOutboxId;
        try {
            failureOutboxId = failureTx.execute(status -> {
                Long outboxId = handler.handleFailure(metadata, typedPayload, code, safeMessage(cause));
                if (outboxId == null) {
                    return null;
                }
                inboxRepository.markProcessed(
                        inboxPo(record, envelope, payloadType, sha, outboxId));
                return outboxId;
            });
        } catch (RuntimeException e) {
            // 拒绝响应本身落库失败 → 不得吞掉，保持可恢复语义让 Kafka 重投
            log.error("FOTA 业务异常后写拒绝响应失败，回滚等待重投：payloadType[{}] messageId[{}] 原因[{}]",
                    payloadType, envelope.getMessageId(), safeMessage(e), e);
            throw asRuntime(cause);
        }

        if (failureOutboxId == null) {
            // 该 payload 不支持拒绝响应：保持原语义（可恢复异常，等待重投/隔离）
            throw asRuntime(cause);
        }
        log.error("FOTA 业务处理异常，已回错误响应：payloadType[{}] messageId[{}] 原因[{}]",
                payloadType, envelope.getMessageId(), safeMessage(cause), cause);
        metrics.increment(KafkaMessagingMetricsService.INBOX_BUSINESS_ERROR);
    }

    private static KafkaInboxPo inboxPo(ConsumerRecord<String, byte[]> record,
                                        VehicleMessageEnvelope envelope,
                                        String payloadType,
                                        String sha,
                                        Long resultMessageId) {
        return KafkaInboxPo.builder()
                .consumerName(payloadType)
                .messageId(envelope.getMessageId())
                .envelopeSha256(sha)
                .payloadType(payloadType)
                .messageKind(envelope.getMessageKind().name())
                .protocolVersion(envelope.getProtocolVersion())
                .vin(envelope.getVin())
                .kafkaTopic(record.topic())
                .kafkaPartition(record.partition())
                .kafkaOffset(record.offset())
                .status(KafkaInboxPo.STATUS_PROCESSED)
                .resultMessageId(resultMessageId)
                .build();
    }

    private static RuntimeException asRuntime(Throwable cause) {
        return cause instanceof RuntimeException runtime
                ? runtime
                : OtaKafkaMessagingException.recoverable(safeMessage(cause), cause);
    }

    @SuppressWarnings("rawtypes")
    private Message parsePayload(FotaPayloadHandler handler, VehicleMessageEnvelope envelope) {
        try {
            return handler.parse(envelope.getPayload());
        } catch (InvalidProtocolBufferException e) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "FOTA payload 解析失败: " + envelope.getPayloadType() + " " + e.getMessage());
        }
    }

    private static String safeMessage(Throwable e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) {
            m = e.getClass().getSimpleName();
        }
        return m.length() > 200 ? m.substring(0, 200) : m;
    }

    /**
     * 业务异常的事务边界信号（携带原始异常，避免被内层事务拦截器标记 rollback-only）。
     */
    private static final class BusinessFailure extends RuntimeException {

        private BusinessFailure(Throwable cause) {
            super(cause);
        }
    }
}
