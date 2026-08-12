package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema;

import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * OTA Kafka Envelope 校验器（CR-013 §3/§5）
 *
 * <p>校验 Envelope 结构、VIN/device 绑定与消息时效。
 * 接入层 MQTT 会话认证与 Topic ACL 不替代 OTA 业务侧的 VIN/device 绑定、消息时效与 schema 校验。
 *
 * @author hwyz_leo
 */
@Component
public class OtaEnvelopeValidator {

    /** 允许的最大消息时效（毫秒）：5 分钟 */
    private static final long MAX_MESSAGE_AGE_MS = 5 * 60 * 1000L;

    /** 未来时间容差（毫秒） */
    private static final long FUTURE_TOLERANCE_MS = 60 * 1000L;

    /**
     * 校验 Envelope 结构。
     *
     * @throws OtaKafkaMessagingException 结构不合法（不可恢复契约错误）
     */
    public void validateStructure(OtaKafkaEnvelope envelope) {
        if (envelope == null) {
            throw OtaKafkaMessagingException.nonRecoverable("Envelope 为空");
        }
        if (envelope.getMessageType() == null || envelope.getMessageType().isBlank()) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 messageType");
        }
        if (envelope.getSchemaVersion() == null) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 schemaVersion");
        }
        if (envelope.getVin() == null || envelope.getVin().isBlank()) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 VIN");
        }
        if (envelope.getDeviceId() == null || envelope.getDeviceId().isBlank()) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 deviceId");
        }
        if (envelope.getPayload() == null || envelope.getPayload().isNull()) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 payload");
        }
    }

    /**
     * 校验消息时效（防重放/过期消息）。
     *
     * @throws OtaKafkaMessagingException 消息过旧或时间戳非法
     */
    public void validateTimestamp(OtaKafkaEnvelope envelope) {
        if (envelope.getTimestamp() == null || envelope.getTimestamp().isBlank()) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 timestamp");
        }
        long messageTime;
        try {
            messageTime = OffsetDateTime.parse(envelope.getTimestamp()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw OtaKafkaMessagingException.nonRecoverable("timestamp 格式非法: " + envelope.getTimestamp());
        }
        long now = Instant.now().toEpochMilli();
        long age = now - messageTime;
        if (age > MAX_MESSAGE_AGE_MS) {
            throw OtaKafkaMessagingException.nonRecoverable("过期消息，时间偏差超限: " + age + "ms");
        }
        if (age < -FUTURE_TOLERANCE_MS) {
            throw OtaKafkaMessagingException.nonRecoverable("消息时间在未来: " + age + "ms");
        }
    }
}
