package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.ExecutionEventCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Execution.ExecutionEvent;
import vehicle.fota.v1.Execution.EventResponse;

/**
 * 安装事件 handler（CR-014 §5：vehicle.fota.v1.ExecutionEvent → EventResponse，EVENT 形态）
 *
 * @author hwyz_leo
 */
@Component
public class ExecutionEventPayloadHandler extends AbstractFotaPayloadHandler<ExecutionEvent> {

    private final ExecutionEventCommandHandler commandHandler;

    public ExecutionEventPayloadHandler(ExecutionEventCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.ExecutionEvent";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_EVENT;
    }

    @Override
    public ExecutionEvent parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return ExecutionEvent.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, ExecutionEvent payload) {
        return metadata.vin() + ":" + payload.getEventId();
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, ExecutionEvent payload) {
        EventResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "EXECUTION", metadata.executionId() == null ? metadata.vin() : metadata.executionId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, ExecutionEvent payload, String reason) {
        EventResponse response = EventResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "EXECUTION", metadata.executionId() == null ? metadata.vin() : metadata.executionId());
    }
}
