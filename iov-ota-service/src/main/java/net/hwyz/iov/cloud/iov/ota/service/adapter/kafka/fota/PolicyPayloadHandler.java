package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.PolicyCommandHandler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Policy.PolicyRequest;
import vehicle.fota.v1.Policy.PolicyResponse;

/**
 * 策略同步 handler（CR-014 §5：vehicle.fota.v1.PolicyRequest → PolicyResponse）
 *
 * @author hwyz_leo
 */
@Component
public class PolicyPayloadHandler extends AbstractFotaPayloadHandler<PolicyRequest> {

    private final PolicyCommandHandler commandHandler;

    public PolicyPayloadHandler(PolicyCommandHandler commandHandler,
              FotaEnvelopeFactory envelopeFactory,
              FotaOutboxAppender outboxAppender,
              KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.commandHandler = commandHandler;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.PolicyRequest";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public PolicyRequest parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return PolicyRequest.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, PolicyRequest payload) {
        return metadata.vin() + ":" + payload.getLocalPolicyVersion();
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, PolicyRequest payload) {
        PolicyResponse response = commandHandler.handle(metadata, payload);
        return appendResponse(metadata, response, "VEHICLE", metadata.vin());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, PolicyRequest payload, String reason) {
        PolicyResponse response = PolicyResponse.newBuilder()
                .setStatus(FotaProtocols.error("CONFLICT", reason))
                .build();
        return appendResponse(metadata, response, "VEHICLE", metadata.vin());
    }
}
