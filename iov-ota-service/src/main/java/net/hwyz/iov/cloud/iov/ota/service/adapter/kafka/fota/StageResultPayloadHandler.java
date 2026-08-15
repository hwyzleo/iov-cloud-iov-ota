package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.StageResultCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Package.StageResultReport;
import vehicle.fota.v1.Package.StageResultResponse;

/**
 * 包阶段结果 handler（CR-014 §5：vehicle.fota.v1.StageResultReport → StageResultResponse）
 *
 * @author hwyz_leo
 */
@Component
public class StageResultPayloadHandler extends AbstractFotaPayloadHandler<StageResultReport> {

    private final StageResultCommandHandler commandHandler;

    public StageResultPayloadHandler(StageResultCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.StageResultReport";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public StageResultReport parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return StageResultReport.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, StageResultReport payload) {
        return metadata.vin() + ":" + payload.getStageResultId();
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, StageResultReport payload) {
        StageResultResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, StageResultReport payload, String reason) {
        StageResultResponse response = StageResultResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }
}
