package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.RecoveryQueryCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.RecoveryResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.RecoveryAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 恢复对账处理器（CR-013 §4：ota.recovery.requested → ota.recovery.resolved）
 *
 * <p>业务唯一键：vin + scope + executionId/vehicleTaskId。
 *
 * @author hwyz_leo
 */
@Component
public class RecoveryRequestedHandler extends AbstractOtaKafkaMessageHandler {

    private final RecoveryAppService recoveryAppService;

    public RecoveryRequestedHandler(ObjectMapper objectMapper,
                                    KafkaOutboxRepository outboxRepository,
                                    KafkaMessagingMetricsService metrics,
                                    RecoveryAppService recoveryAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.recoveryAppService = recoveryAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.RECOVERY_REQUESTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        RecoveryQueryCmd cmd = treeToValue(payload, RecoveryQueryCmd.class);
        return envelope.getVin() + ":" + cmd.getScope() + ":" + cmd.getExecutionId() + ":" + cmd.getVehicleTaskId();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        RecoveryQueryCmd cmd = treeToValue(payload, RecoveryQueryCmd.class);
        cmd.setVin(envelope.getVin());
        RecoveryResult result = recoveryAppService.query(cmd);
        metrics.increment("ota.recovery.query");
        if ("MANUAL_RECOVERY_REQUIRED".equals(result.getDisposition())) {
            metrics.increment(KafkaMessagingMetricsService.RECOVERY_FAILED);
        }
        String aggregateId = cmd.getExecutionId() != null
                ? String.valueOf(cmd.getExecutionId()) : String.valueOf(cmd.getVehicleTaskId());
        return appendResult("EXECUTION", aggregateId,
                OtaMessageType.RECOVERY_RESOLVED, envelope.getVin(), envelope, result);
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        RecoveryQueryCmd cmd = treeToValue(payload, RecoveryQueryCmd.class);
        String aggregateId = cmd.getExecutionId() != null
                ? String.valueOf(cmd.getExecutionId()) : String.valueOf(cmd.getVehicleTaskId());
        return appendRejected("EXECUTION", aggregateId,
                OtaMessageType.RECOVERY_RESOLVED, envelope.getVin(), envelope,
                "OTA-RECOVERY-CONFLICT", reason);
    }
}
