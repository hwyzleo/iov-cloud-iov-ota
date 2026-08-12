package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CR-013 Envelope 校验器单元测试
 *
 * @author hwyz_leo
 */
@DisplayName("OtaEnvelopeValidator - Envelope 结构与时效校验")
class OtaEnvelopeValidatorTest {

    private final OtaEnvelopeValidator validator = new OtaEnvelopeValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private OtaKafkaEnvelope validEnvelope;

    @BeforeEach
    void setUp() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("vin", "VIN001");
        validEnvelope = OtaKafkaEnvelope.builder()
                .messageId("msg-001")
                .messageType(OtaMessageType.TASK_DETECT_REQUESTED.getValue())
                .schemaVersion(1)
                .timestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                .deviceId("CGW-001")
                .vin("VIN001")
                .correlationId("corr-001")
                .payloadDigest("sha256:abc")
                .payload(payload)
                .build();
    }

    @Test
    @DisplayName("合法 Envelope 通过结构与时效校验")
    void validEnvelope_passes() {
        assertDoesNotThrow(() -> validator.validateStructure(validEnvelope));
        assertDoesNotThrow(() -> validator.validateTimestamp(validEnvelope));
    }

    @Test
    @DisplayName("缺少 VIN 视为不可恢复契约错误")
    void missingVin_nonRecoverable() {
        validEnvelope.setVin(null);
        OtaKafkaMessagingException e = assertThrows(OtaKafkaMessagingException.class,
                () -> validator.validateStructure(validEnvelope));
        assertFalse(e.isRecoverable());
    }

    @Test
    @DisplayName("缺少 deviceId 视为不可恢复契约错误")
    void missingDeviceId_nonRecoverable() {
        validEnvelope.setDeviceId(null);
        assertThrows(OtaKafkaMessagingException.class, () -> validator.validateStructure(validEnvelope));
    }

    @Test
    @DisplayName("缺少 payload 视为不可恢复契约错误")
    void missingPayload_nonRecoverable() {
        validEnvelope.setPayload(null);
        assertThrows(OtaKafkaMessagingException.class, () -> validator.validateStructure(validEnvelope));
    }

    @Test
    @DisplayName("过期消息（超过 5 分钟）被拒绝")
    void staleMessage_rejected() {
        validEnvelope.setTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.now().minusSeconds(600)));
        OtaKafkaMessagingException e = assertThrows(OtaKafkaMessagingException.class,
                () -> validator.validateTimestamp(validEnvelope));
        assertFalse(e.isRecoverable());
    }

    @Test
    @DisplayName("非法时间戳格式被拒绝")
    void illegalTimestamp_rejected() {
        validEnvelope.setTimestamp("not-a-timestamp");
        assertThrows(OtaKafkaMessagingException.class, () -> validator.validateTimestamp(validEnvelope));
    }

    @Test
    @DisplayName("缺少 timestamp 被拒绝")
    void missingTimestamp_rejected() {
        validEnvelope.setTimestamp(null);
        assertThrows(OtaKafkaMessagingException.class, () -> validator.validateTimestamp(validEnvelope));
    }
}
