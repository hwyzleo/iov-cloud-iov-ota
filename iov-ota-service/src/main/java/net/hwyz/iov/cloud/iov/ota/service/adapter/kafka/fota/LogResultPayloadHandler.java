package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.LogResultCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Log.LogUploadResult;
import vehicle.fota.v1.Log.LogResultResponse;

/**
 * 日志上传结果 handler（CR-014 §5：vehicle.fota.v1.LogUploadResult → LogResultResponse）
 *
 * @author hwyz_leo
 */
@Component
public class LogResultPayloadHandler extends AbstractFotaPayloadHandler<LogUploadResult> {

    private final LogResultCommandHandler commandHandler;

    public LogResultPayloadHandler(LogResultCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.LogUploadResult";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public LogUploadResult parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return LogUploadResult.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, LogUploadResult payload) {
        return metadata.vin() + ":" + payload.getObjectKey();
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, LogUploadResult payload) {
        LogResultResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, LogUploadResult payload, String reason) {
        LogResultResponse response = LogResultResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "VEHICLE_TASK", metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }
}
