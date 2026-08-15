package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto.ParProtoReleaseGuard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.common.v1.Envelope.VehicleMessageEnvelope;
import vehicle.fota.v1.Task.TaskCheckResponse;
import vehicle.fota.v1.Types.ControlCommand;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FOTA 出站 Envelope 工厂单元测试（CR-014 §6、验收 4/5）
 *
 * @author hwyz_leo
 */
@DisplayName("FotaEnvelopeFactory - 出站 Envelope 冻结与关联规则")
class FotaEnvelopeFactoryTest {

    private static FotaEnvelopeFactory factory;

    @BeforeAll
    static void setUp() {
        ParProtoReleaseGuard guard = new ParProtoReleaseGuard(new ObjectMapper());
        guard.verify();
        factory = new FotaEnvelopeFactory(guard);
    }

    @Test
    @DisplayName("RESPONSE：继承 request_id、correlation_id 指向请求 message_id、payload 强类型")
    void response_envelope_rules() throws InvalidProtocolBufferException {
        FotaMessageMetadata request = new FotaMessageMetadata(
                "req-1", 1000L, "fota-1", "dev-1", "LSVAU2188N2ZG4G",
                "1001", "2002", "ik-1", "vehicle.fota.v1.TaskCheckRequest",
                "msg-1", null, MessageKind.MESSAGE_KIND_REQUEST, null, null);

        TaskCheckResponse response = TaskCheckResponse.newBuilder()
                .setNextAction("PROCEED")
                .build();
        FotaOutboundEnvelope out = factory.response(request, response, "VEHICLE", "LSVAU2188N2ZG4G");

        assertEquals("vehicle.fota.v1.TaskCheckResponse", out.payloadType());
        assertEquals("RESPONSE", out.messageKind());
        assertEquals("msg-1", out.correlationId(), "RESPONSE correlation_id 必须指向请求 message_id");
        assertEquals("LSVAU2188N2ZG4G", out.vin());

        // bytes 可解析且 payload 强类型回读
        VehicleMessageEnvelope envelope = VehicleMessageEnvelope.parseFrom(out.envelopeBytes());
        assertEquals("req-1", envelope.getRequestId());
        assertEquals("msg-1", envelope.getCorrelationId());
        assertEquals(FotaEnvelopeValidator.FOTA_SERVICE, envelope.getService());
        assertEquals(TaskCheckResponse.getDefaultInstance().getDescriptorForType().getFullName(),
                envelope.getPayloadType());
        assertEquals(TaskCheckResponse.parseFrom(envelope.getPayload()).getNextAction(), "PROCEED");
        // 摘要一致性
        assertEquals(FotaDigests.sha256(out.envelopeBytes()), out.envelopeSha256());
    }

    @Test
    @DisplayName("EVENT：稳定 message_id、message_kind=EVENT、payload=ControlCommand")
    void event_envelope_rules() throws InvalidProtocolBufferException {
        ControlCommand control = ControlCommand.newBuilder()
                .setControlId("ctl-1")
                .setControlRevision(1L)
                .setIssuedAtMs(1000L)
                .setExpiresAtMs(2000L)
                .build();
        FotaOutboundEnvelope out = factory.event("LSVAU2188N2ZG4G", "dev-1", "req-9",
                "1001", "2002", "ik-9", control, "EXECUTION", "2002");

        assertEquals("EVENT", out.messageKind());
        assertEquals("vehicle.fota.v1.ControlCommand", out.payloadType());
        assertEquals("LSVAU2188N2ZG4G", out.vin());
        VehicleMessageEnvelope envelope = VehicleMessageEnvelope.parseFrom(out.envelopeBytes());
        assertEquals("req-9", envelope.getRequestId());
        assertEquals("1001", envelope.getVehicleTaskId());
        assertEquals("2002", envelope.getExecutionId());
        assertEquals("ik-9", envelope.getIdempotencyKey());
        assertEquals(ControlCommand.parseFrom(envelope.getPayload()).getControlId(), "ctl-1");
    }
}
