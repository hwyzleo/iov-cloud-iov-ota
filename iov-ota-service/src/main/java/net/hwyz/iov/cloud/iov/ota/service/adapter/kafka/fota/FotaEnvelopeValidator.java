package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto.ParProtoReleaseGuard;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto.PayloadTypeEntry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.common.v1.Envelope.VehicleMessageEnvelope;

import java.time.Instant;

/**
 * FOTA Envelope 强类型校验器（CR-014 §4.2）
 *
 * <p>校验：Key=Envelope.vin、service=vehicle.fota、protocol major、必填字段、
 * message_kind 非 UNSPECIFIED、TTL/时效、payload 大小、payload_type∈registry、
 * message_kind 方向矩阵。校验失败不得尝试 JSON、旧 Envelope、ota.* 或 schemaVersion fallback。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class FotaEnvelopeValidator {

    /** FOTA 业务域标识（稳定、版本无关） */
    public static final String FOTA_SERVICE = "vehicle.fota";

    /** payload 大小上限（字节） */
    private static final long MAX_PAYLOAD_BYTES = 1024 * 1024;

    /** 最大消息时效（毫秒）：5 分钟 */
    private static final long MAX_MESSAGE_AGE_MS = 5 * 60 * 1000L;

    /** 未来时间容差（毫秒） */
    private static final long FUTURE_TOLERANCE_MS = 60 * 1000L;

    private final ParProtoReleaseGuard releaseGuard;

    /**
     * 校验一条上行 record 的 Envelope。
     *
     * @throws OtaKafkaMessagingException 不可恢复契约错误
     */
    public void validate(ConsumerRecord<String, byte[]> record, VehicleMessageEnvelope envelope) {
        if (envelope == null) {
            throw OtaKafkaMessagingException.nonRecoverable("Envelope 为空");
        }
        // 1. Kafka Key 必须等于 Envelope.vin
        if (record.key() == null || !record.key().equals(envelope.getVin())) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "Kafka Key 与 Envelope.vin 不一致: key[" + maskVin(record.key()) + "] vin["
                            + maskVin(envelope.getVin()) + "]");
        }
        // 2. service 必须为 vehicle.fota
        if (!FOTA_SERVICE.equals(envelope.getService())) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "非法 service: " + envelope.getService());
        }
        // 3. protocol_version：不透明版本串，按 SSOT canonical 整串精确匹配（禁止解析数值 major）
        String expectedVersion = releaseGuard.registry().getProtocolVersion();
        if (!expectedVersion.equals(envelope.getProtocolVersion())) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "protocol_version 不支持: " + envelope.getProtocolVersion()
                            + "（期望 " + expectedVersion + "）");
        }
        // 4. 必填字段
        if (isBlank(envelope.getMessageId())) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 message_id");
        }
        if (isBlank(envelope.getRequestId())) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 request_id");
        }
        if (isBlank(envelope.getDeviceId())) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 device_id");
        }
        if (isBlank(envelope.getVin())) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 vin");
        }
        if (isBlank(envelope.getPayloadType())) {
            throw OtaKafkaMessagingException.nonRecoverable("缺少 payload_type");
        }
        if (envelope.getTimestampMs() <= 0) {
            throw OtaKafkaMessagingException.nonRecoverable("timestamp_ms 非法");
        }
        // 5. message_kind 非 UNSPECIFIED
        if (envelope.getMessageKind() == null
                || envelope.getMessageKind() == MessageKind.MESSAGE_KIND_UNSPECIFIED) {
            throw OtaKafkaMessagingException.nonRecoverable("message_kind 未指定");
        }
        // 6. TTL/时效
        validateTtl(envelope);
        // 7. payload 大小
        if (envelope.getPayload().size() > MAX_PAYLOAD_BYTES) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "payload 超过大小上限: " + envelope.getPayload().size());
        }
        // 8. payload_type ∈ registry + 方向矩阵
        validatePayloadType(envelope);
    }

    private void validateTtl(VehicleMessageEnvelope envelope) {
        long now = Instant.now().toEpochMilli();
        if (envelope.hasExpireAtMs() && envelope.getExpireAtMs() <= now) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "消息已过期: expire_at_ms=" + envelope.getExpireAtMs());
        }
        long age = now - envelope.getTimestampMs();
        if (age > MAX_MESSAGE_AGE_MS) {
            throw OtaKafkaMessagingException.nonRecoverable("过期消息，时间偏差超限: " + age + "ms");
        }
        if (age < -FUTURE_TOLERANCE_MS) {
            throw OtaKafkaMessagingException.nonRecoverable("消息时间在未来: " + age + "ms");
        }
    }

    private void validatePayloadType(VehicleMessageEnvelope envelope) {
        PayloadTypeEntry entry = releaseGuard.registry().resolve(envelope.getPayloadType());
        if (entry == null) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "payload_type 未注册: " + envelope.getPayloadType());
        }
        // 上行 Envelope 只能承载 INBOUND 方向且 message_kind 必须与注册表一致
        if (!entry.isInbound()) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "payload_type 不允许上行: " + envelope.getPayloadType());
        }
        if (!entry.messageKind().equals(normalizeKind(envelope.getMessageKind()))) {
            throw OtaKafkaMessagingException.nonRecoverable(
                    "message_kind 与方向矩阵不一致: " + envelope.getPayloadType() + " 期望["
                            + entry.messageKind() + "] 实际[" + envelope.getMessageKind().name() + "]");
        }
    }

    /** proto 枚举名（MESSAGE_KIND_REQUEST）→ 短名（REQUEST） */
    private static String normalizeKind(MessageKind kind) {
        if (kind == null) {
            return null;
        }
        String name = kind.name();
        return name.startsWith("MESSAGE_KIND_") ? name.substring("MESSAGE_KIND_".length()) : name;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 日志脱敏：仅输出 VIN 末 4 位。 */
    public static String maskVin(String vin) {
        if (vin == null || vin.length() <= 4) {
            return "***";
        }
        return "***" + vin.substring(vin.length() - 4);
    }
}
