package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.ConsentCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Consent.ConsentReport;
import vehicle.fota.v1.Consent.ConsentResponse;

/**
 * 授权/撤回 handler（CR-014 §5：vehicle.fota.v1.ConsentReport → ConsentResponse）
 *
 * @author hwyz_leo
 */
@Component
public class ConsentPayloadHandler extends AbstractFotaPayloadHandler<ConsentReport> {

    private final ConsentCommandHandler commandHandler;

    public ConsentPayloadHandler(ConsentCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.ConsentReport";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public ConsentReport parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return ConsentReport.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, ConsentReport payload) {
        // 幂等身份优先取 Envelope idempotencyKey / messageId（CR-016 §3.2/§5）
        if (metadata.idempotencyKey() != null && !metadata.idempotencyKey().isBlank()) {
            return metadata.vin() + ":" + metadata.idempotencyKey();
        }
        return metadata.vin() + ":" + metadata.messageId();
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, ConsentReport payload) {
        ConsentResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, ConsentReport payload, String reason) {
        ConsentResponse response = ConsentResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }
}
