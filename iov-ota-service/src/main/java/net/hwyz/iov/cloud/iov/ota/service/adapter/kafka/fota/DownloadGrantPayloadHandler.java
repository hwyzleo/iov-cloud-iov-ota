package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.DownloadGrantCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Package.DownloadGrantRequest;
import vehicle.fota.v1.Package.DownloadGrantResponse;

/**
 * 下载授权 handler（CR-014 §5：vehicle.fota.v1.DownloadGrantRequest → DownloadGrantResponse）
 *
 * @author hwyz_leo
 */
@Component
public class DownloadGrantPayloadHandler extends AbstractFotaPayloadHandler<DownloadGrantRequest> {

    private final DownloadGrantCommandHandler commandHandler;

    public DownloadGrantPayloadHandler(DownloadGrantCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.DownloadGrantRequest";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public DownloadGrantRequest parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return DownloadGrantRequest.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, DownloadGrantRequest payload) {
        return metadata.vin() + ":" + payload.getPackageId() + ":" + (payload.hasCurrentOffsetBytes() ? payload.getCurrentOffsetBytes() : 0L);
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, DownloadGrantRequest payload) {
        DownloadGrantResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, DownloadGrantRequest payload, String reason) {
        DownloadGrantResponse response = DownloadGrantResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }
}
