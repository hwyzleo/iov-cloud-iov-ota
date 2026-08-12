package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ControlAckCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionEventAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 控制回执处理器（CR-013 §4：ota.execution.control-ack.reported → ota.execution.control-ack.acknowledged）
 *
 * <p>业务唯一键：controlAckId。
 *
 * @author hwyz_leo
 */
@Component
public class ExecutionControlAckReportedHandler extends AbstractOtaKafkaMessageHandler {

    private final ExecutionEventAppService executionEventAppService;

    public ExecutionControlAckReportedHandler(ObjectMapper objectMapper,
                                              KafkaOutboxRepository outboxRepository,
                                              KafkaMessagingMetricsService metrics,
                                              ExecutionEventAppService executionEventAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.executionEventAppService = executionEventAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.EXECUTION_CONTROL_ACK_REPORTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        ControlAckCmd cmd = treeToValue(payload, ControlAckCmd.class);
        return envelope.getVin() + ":" + cmd.getControlAckId();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        ControlAckCmd cmd = treeToValue(payload, ControlAckCmd.class);
        cmd.setVin(envelope.getVin());
        executionEventAppService.receiveControlAck(cmd);
        metrics.increment(KafkaMessagingMetricsService.EXECUTION_CONTROL_ACK);
        return appendResult("EXECUTION", String.valueOf(cmd.getExecutionId()),
                OtaMessageType.EXECUTION_CONTROL_ACK_ACKNOWLEDGED, String.valueOf(cmd.getExecutionId()), envelope,
                java.util.Map.of("controlAckId", cmd.getControlAckId(), "ackStatus", cmd.getAckStatus(),
                        "acknowledged", true));
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        ControlAckCmd cmd = treeToValue(payload, ControlAckCmd.class);
        return appendRejected("EXECUTION", String.valueOf(cmd.getExecutionId()),
                OtaMessageType.EXECUTION_CONTROL_ACK_ACKNOWLEDGED, String.valueOf(cmd.getExecutionId()), envelope,
                "OTA-CONTROL-ACK-CONFLICT", reason);
    }
}
