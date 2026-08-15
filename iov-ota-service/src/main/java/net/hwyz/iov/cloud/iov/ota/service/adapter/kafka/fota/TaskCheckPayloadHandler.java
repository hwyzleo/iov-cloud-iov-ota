package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.TaskCheckCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Task.TaskCheckRequest;
import vehicle.fota.v1.Task.TaskCheckResponse;

/**
 * 任务检测 handler（CR-014 §5：vehicle.fota.v1.TaskCheckRequest → TaskCheckResponse）
 *
 * @author hwyz_leo
 */
@Component
public class TaskCheckPayloadHandler extends AbstractFotaPayloadHandler<TaskCheckRequest> {

    private final TaskCheckCommandHandler commandHandler;

    public TaskCheckPayloadHandler(TaskCheckCommandHandler commandHandler,
                                   FotaEnvelopeFactory envelopeFactory,
                                   FotaOutboxAppender outboxAppender,
                                   KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.TaskCheckRequest";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public TaskCheckRequest parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return TaskCheckRequest.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, TaskCheckRequest payload) {
        String digest = payload.hasEcuListDigest() ? payload.getEcuListDigest().getValueHex() : "";
        return metadata.vin() + ":" + payload.getInventoryMode() + ":" + payload.getInventoryRevision() + ":" + digest;
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, TaskCheckRequest payload) {
        metrics.increment("ota.detect.total");
        TaskCheckResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "VEHICLE", metadata.vin());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, TaskCheckRequest payload, String reason) {
        TaskCheckResponse response = TaskCheckResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .setNextAction("WAIT")
                .build();
        return appendResponse(metadata, response, "VEHICLE", metadata.vin());
    }
}
