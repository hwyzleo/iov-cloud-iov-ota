package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.LogGrantCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Log.LogGrantRequest;
import vehicle.fota.v1.Log.LogGrantResponse;

/**
 * 日志上传凭证 handler（CR-014 §5：vehicle.fota.v1.LogGrantRequest → LogGrantResponse）
 *
 * @author hwyz_leo
 */
@Component
public class LogGrantPayloadHandler extends AbstractFotaPayloadHandler<LogGrantRequest> {

    private final LogGrantCommandHandler commandHandler;

    public LogGrantPayloadHandler(LogGrantCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.LogGrantRequest";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public LogGrantRequest parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return LogGrantRequest.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, LogGrantRequest payload) {
        return metadata.vin() + ":log-grant:" + payload.getFileName();
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, LogGrantRequest payload) {
        LogGrantResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, LogGrantRequest payload, String reason) {
        LogGrantResponse response = LogGrantResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }
}
