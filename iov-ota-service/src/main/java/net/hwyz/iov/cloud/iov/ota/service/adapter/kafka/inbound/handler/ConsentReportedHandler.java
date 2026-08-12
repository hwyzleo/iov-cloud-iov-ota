package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ConsentCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ConsentResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ConsentAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 用户授权/撤回处理器（CR-013 §4：ota.consent.reported → ota.consent.recorded / ota.consent.rejected）
 *
 * <p>业务唯一键：优先 consentReceiptId（撤回/幂等），否则 vehicleTaskId + action。
 *
 * @author hwyz_leo
 */
@Component
public class ConsentReportedHandler extends AbstractOtaKafkaMessageHandler {

    private final ConsentAppService consentAppService;

    public ConsentReportedHandler(ObjectMapper objectMapper,
                                  KafkaOutboxRepository outboxRepository,
                                  KafkaMessagingMetricsService metrics,
                                  ConsentAppService consentAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.consentAppService = consentAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.CONSENT_REPORTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        ConsentCmd cmd = treeToValue(payload, ConsentCmd.class);
        if (cmd.getConsentReceiptId() != null && !cmd.getConsentReceiptId().isBlank()) {
            return envelope.getVin() + ":" + cmd.getConsentReceiptId();
        }
        return envelope.getVin() + ":" + cmd.getVehicleTaskId() + ":" + cmd.getAction();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        ConsentCmd cmd = treeToValue(payload, ConsentCmd.class);
        cmd.setVin(envelope.getVin());
        ConsentResult result = consentAppService.handleConsent(cmd);
        metrics.increment("ota.consent.total");
        return appendResult("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.CONSENT_RECORDED, envelope.getVin(), envelope, result);
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        ConsentCmd cmd = treeToValue(payload, ConsentCmd.class);
        return appendRejected("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.CONSENT_REJECTED, envelope.getVin(), envelope, "OTA-CONSENT-CONFLICT", reason);
    }
}
