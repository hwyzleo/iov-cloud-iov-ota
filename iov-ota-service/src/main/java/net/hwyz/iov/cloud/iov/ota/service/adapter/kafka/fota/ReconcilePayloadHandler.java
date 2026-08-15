package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.ReconcileCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Reconcile.ReconcileRequest;
import vehicle.fota.v1.Reconcile.ReconcileResponse;

/**
 * 状态对账 handler（CR-014 §5：vehicle.fota.v1.ReconcileRequest → ReconcileResponse）
 *
 * @author hwyz_leo
 */
@Component
public class ReconcilePayloadHandler extends AbstractFotaPayloadHandler<ReconcileRequest> {

    private final ReconcileCommandHandler commandHandler;

    public ReconcilePayloadHandler(ReconcileCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.ReconcileRequest";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public ReconcileRequest parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return ReconcileRequest.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, ReconcileRequest payload) {
        return metadata.vin() + ":" + payload.getQueryScope() + ":" + metadata.executionId() + ":" + metadata.vehicleTaskId();
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, ReconcileRequest payload) {
        ReconcileResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "EXECUTION", metadata.executionId() == null ? metadata.vin() : metadata.executionId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, ReconcileRequest payload, String reason) {
        ReconcileResponse response = ReconcileResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "EXECUTION", metadata.executionId() == null ? metadata.vin() : metadata.executionId());
    }
}
