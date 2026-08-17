package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto.ParProtoReleaseGuard;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.common.v1.Envelope.VehicleMessageEnvelope;
import vehicle.fota.v1.Task.TaskCheckRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FOTA Envelope 强类型校验器单元测试（CR-014 §4.2、验收 3）
 *
 * @author hwyz_leo
 */
@DisplayName("FotaEnvelopeValidator - Envelope 强类型校验（负例）")
class FotaEnvelopeValidatorTest {

    private static FotaEnvelopeValidator validator;

    @BeforeAll
    static void setUp() {
        ParProtoReleaseGuard guard = new ParProtoReleaseGuard(new ObjectMapper());
        guard.verify();
        validator = new FotaEnvelopeValidator(guard);
    }

    @Test
    @DisplayName("合法 TaskCheckRequest Envelope 校验通过")
    void valid_envelope_passes() {
        VehicleMessageEnvelope envelope = validEnvelope();
        validator.validate(record(envelope.getVin(), envelope), envelope);
    }

    @Test
    @DisplayName("Kafka Key ≠ Envelope.vin → 不可恢复契约错误")
    void key_mismatch_fails() {
        ConsumerRecord<String, byte[]> record = record("OTHER", validEnvelope());
        assertNonRecoverable(() -> validator.validate(record, validEnvelope()));
    }

    @Test
    @DisplayName("service ≠ vehicle.fota → 不可恢复契约错误")
    void wrong_service_fails() {
        VehicleMessageEnvelope e = envelopeBuilder().setService("vehicle.ota").build();
        assertNonRecoverable(() -> validator.validate(record(e.getVin(), e), e));
    }

    @Test
    @DisplayName("protocol major 不支持 → 不可恢复契约错误")
    void wrong_protocol_major_fails() {
        VehicleMessageEnvelope e = envelopeBuilder().setProtocolVersion("fota-v9").build();
        assertNonRecoverable(() -> validator.validate(record(e.getVin(), e), e));
    }

    @Test
    @DisplayName("payload_type 未注册（旧 ota.* / 未知类型）→ fail-closed")
    void unknown_payload_type_fails() {
        VehicleMessageEnvelope e = envelopeBuilder().setPayloadType("ota.task.detect.requested").build();
        assertNonRecoverable(() -> validator.validate(record(e.getVin(), e), e));
    }

    @Test
    @DisplayName("message_kind 与方向矩阵不一致（REQUEST 类型用 EVENT）→ fail-closed")
    void message_kind_mismatch_fails() {
        VehicleMessageEnvelope e = envelopeBuilder().setMessageKind(MessageKind.MESSAGE_KIND_EVENT).build();
        assertNonRecoverable(() -> validator.validate(record(e.getVin(), e), e));
    }

    @Test
    @DisplayName("过期消息（TTL/时效超限）→ 不可恢复契约错误")
    void expired_message_fails() {
        VehicleMessageEnvelope e = envelopeBuilder()
                .setTimestampMs(System.currentTimeMillis() - 10 * 60 * 1000L)
                .build();
        assertNonRecoverable(() -> validator.validate(record(e.getVin(), e), e));
    }

    @Test
    @DisplayName("VIN 脱敏只暴露末 4 位")
    void vin_mask() {
        assertEquals("***ZG4G", FotaEnvelopeValidator.maskVin("LSVAU2188N2ZG4G"));
        assertEquals("***", FotaEnvelopeValidator.maskVin("VIN"));
    }

    // ------------------------------------------------------------------ helpers

    private static VehicleMessageEnvelope validEnvelope() {
        TaskCheckRequest payload = TaskCheckRequest.newBuilder()
                .setInventoryMode(vehicle.fota.v1.Types.InventoryMode.INVENTORY_MODE_DIGEST)
                .setInventoryRevision(3L)
                .build();
        return envelopeBuilder()
                .setPayload(payload.toByteString())
                .setPayloadType("vehicle.fota.v1.TaskCheckRequest")
                .build();
    }

    private static VehicleMessageEnvelope.Builder envelopeBuilder() {
        return VehicleMessageEnvelope.newBuilder()
                .setRequestId("req-1")
                .setTimestampMs(System.currentTimeMillis())
                .setProtocolVersion("fota-v1")
                .setDeviceId("dev-1")
                .setVin("LSVAU2188N2ZG4G")
                .setMessageId("msg-1")
                .setMessageKind(MessageKind.MESSAGE_KIND_REQUEST)
                .setService("vehicle.fota");
    }

    private static ConsumerRecord<String, byte[]> record(String key, VehicleMessageEnvelope envelope) {
        return new ConsumerRecord<>("iov.vagw.up.fota", 0, 0L, key, envelope.toByteArray());
    }

    private static void assertNonRecoverable(Runnable runnable) {
        OtaKafkaMessagingException ex = assertThrows(OtaKafkaMessagingException.class, runnable::run);
        assertFalse(ex.isRecoverable(), "契约错误应为不可恢复");
    }
}
