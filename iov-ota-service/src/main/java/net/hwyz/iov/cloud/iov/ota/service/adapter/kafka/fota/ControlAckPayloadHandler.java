package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.ControlAckCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Execution.ControlAckReport;
import vehicle.fota.v1.Execution.ControlAckResponse;

/**
 * 控制回执 handler（CR-014 §5：vehicle.fota.v1.ControlAckReport → ControlAckResponse）
 *
 * @author hwyz_leo
 */
@Component
public class ControlAckPayloadHandler extends AbstractFotaPayloadHandler<ControlAckReport> {

    private final ControlAckCommandHandler commandHandler;

    public ControlAckPayloadHandler(ControlAckCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.ControlAckReport";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public ControlAckReport parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return ControlAckReport.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, ControlAckReport payload) {
        return metadata.vin() + ":" + (payload.hasAck() ? payload.getAck().getControlAckId() : "");
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, ControlAckReport payload) {
        ControlAckResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "EXECUTION", metadata.executionId() == null ? metadata.vin() : metadata.executionId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, ControlAckReport payload, String reason) {
        ControlAckResponse response = ControlAckResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "EXECUTION", metadata.executionId() == null ? metadata.vin() : metadata.executionId());
    }
}
