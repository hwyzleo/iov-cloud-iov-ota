package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FinalResultCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Execution.FinalResultReport;
import vehicle.fota.v1.Execution.FinalResultResponse;

/**
 * 最终结果收口 handler（CR-014 §5：vehicle.fota.v1.FinalResultReport → FinalResultResponse）
 *
 * @author hwyz_leo
 */
@Component
public class FinalResultPayloadHandler extends AbstractFotaPayloadHandler<FinalResultReport> {

    private final FinalResultCommandHandler commandHandler;

    public FinalResultPayloadHandler(FinalResultCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.FinalResultReport";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public FinalResultReport parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return FinalResultReport.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, FinalResultReport payload) {
        return metadata.idempotencyKey() != null ? metadata.vin() + ":" + metadata.idempotencyKey() : metadata.vin() + ":" + metadata.executionId() + ":" + (payload.hasResultDigest() ? payload.getResultDigest().getValueHex() : "");
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, FinalResultReport payload) {
        FinalResultResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "EXECUTION", metadata.executionId() == null ? metadata.vin() : metadata.executionId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, FinalResultReport payload, String reason) {
        FinalResultResponse response = FinalResultResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "EXECUTION", metadata.executionId() == null ? metadata.vin() : metadata.executionId());
    }
}
