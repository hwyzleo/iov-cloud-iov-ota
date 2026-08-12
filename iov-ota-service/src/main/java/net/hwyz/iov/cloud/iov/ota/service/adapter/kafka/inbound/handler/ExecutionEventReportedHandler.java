package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionEventCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionEventResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionEventAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 安装事件处理器（CR-013 §4：ota.execution.event.reported → ota.execution.event.acknowledged）
 *
 * <p>顺序事件、连续水位与缺失范围（US-080）。
 * 业务唯一键：eventId（配合 (executionId, sequenceNo) 双重唯一键幂等）。
 * Kafka key 使用 executionId，保证同一执行分区内有序。
 *
 * @author hwyz_leo
 */
@Component
public class ExecutionEventReportedHandler extends AbstractOtaKafkaMessageHandler {

    private final ExecutionEventAppService executionEventAppService;

    public ExecutionEventReportedHandler(ObjectMapper objectMapper,
                                         KafkaOutboxRepository outboxRepository,
                                         KafkaMessagingMetricsService metrics,
                                         ExecutionEventAppService executionEventAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.executionEventAppService = executionEventAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.EXECUTION_EVENT_REPORTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        ExecutionEventCmd cmd = treeToValue(payload, ExecutionEventCmd.class);
        return envelope.getVin() + ":" + cmd.getEventId();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        ExecutionEventCmd cmd = treeToValue(payload, ExecutionEventCmd.class);
        cmd.setVin(envelope.getVin());
        ExecutionEventResult result = executionEventAppService.receiveEvent(cmd);
        if ("BUFFERED".equals(result.getDisposition())) {
            metrics.increment("ota.event.buffered");
        } else if ("DUPLICATE".equals(result.getDisposition())) {
            metrics.increment("ota.event.duplicate");
        }
        // Kafka key 使用 executionId，保证同一执行分区内有序
        return appendResult("EXECUTION", String.valueOf(cmd.getExecutionId()),
                OtaMessageType.EXECUTION_EVENT_ACKNOWLEDGED, String.valueOf(cmd.getExecutionId()), envelope, result);
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        ExecutionEventCmd cmd = treeToValue(payload, ExecutionEventCmd.class);
        return appendRejected("EXECUTION", String.valueOf(cmd.getExecutionId()),
                OtaMessageType.EXECUTION_EVENT_ACKNOWLEDGED, String.valueOf(cmd.getExecutionId()), envelope,
                "OTA-EVENT-CONFLICT", reason);
    }
}
