package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.PolicySyncCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.PolicySyncResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.PolicySyncAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 策略同步处理器（CR-013 §4：ota.policy.sync.requested → ota.policy.synced / ota.policy.conflicted）
 *
 * <p>业务唯一键：vin + basePreferenceVersion（乐观并发）。
 *
 * @author hwyz_leo
 */
@Component
public class PolicySyncRequestedHandler extends AbstractOtaKafkaMessageHandler {

    private final PolicySyncAppService policySyncAppService;

    public PolicySyncRequestedHandler(ObjectMapper objectMapper,
                                      KafkaOutboxRepository outboxRepository,
                                      KafkaMessagingMetricsService metrics,
                                      PolicySyncAppService policySyncAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.policySyncAppService = policySyncAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.POLICY_SYNC_REQUESTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        PolicySyncCmd cmd = treeToValue(payload, PolicySyncCmd.class);
        return envelope.getVin() + ":" + cmd.getBasePreferenceVersion();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        PolicySyncCmd cmd = treeToValue(payload, PolicySyncCmd.class);
        cmd.setVin(envelope.getVin());
        PolicySyncResult result = policySyncAppService.sync(cmd);
        metrics.increment("ota.policy.sync");
        if (result.isRevisionUpgradeRequired()) {
            return appendResult("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                    OtaMessageType.POLICY_CONFLICTED, envelope.getVin(), envelope, result);
        }
        return appendResult("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.POLICY_SYNCED, envelope.getVin(), envelope, result);
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        PolicySyncCmd cmd = treeToValue(payload, PolicySyncCmd.class);
        return appendRejected("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.POLICY_CONFLICTED, envelope.getVin(), envelope,
                "OTA-POLICY-CONFLICT", reason);
    }
}
