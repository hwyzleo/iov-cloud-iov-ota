package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionCreateCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionCreateResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 安装许可申请处理器（CR-013 §4：ota.execution.permit.requested → ota.execution.permitted / ota.execution.permit-denied）
 *
 * <p>申请安装并创建 Execution（US-079）。
 * 业务唯一键：优先 idempotencyKey（车辆跨消息重试同一安装申请），否则 vin + vehicleTaskId。
 *
 * @author hwyz_leo
 */
@Component
public class ExecutionPermitRequestedHandler extends AbstractOtaKafkaMessageHandler {

    private final ExecutionAppService executionAppService;

    public ExecutionPermitRequestedHandler(ObjectMapper objectMapper,
                                           KafkaOutboxRepository outboxRepository,
                                           KafkaMessagingMetricsService metrics,
                                           ExecutionAppService executionAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.executionAppService = executionAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.EXECUTION_PERMIT_REQUESTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        ExecutionCreateCmd cmd = treeToValue(payload, ExecutionCreateCmd.class);
        if (cmd.getIdempotencyKey() != null && !cmd.getIdempotencyKey().isBlank()) {
            return envelope.getVin() + ":" + cmd.getIdempotencyKey();
        }
        return envelope.getVin() + ":" + cmd.getVehicleTaskId();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        ExecutionCreateCmd cmd = treeToValue(payload, ExecutionCreateCmd.class);
        cmd.setVin(envelope.getVin());
        ExecutionCreateResult result = executionAppService.requestInstall(cmd);
        metrics.increment("ota.execution.permit");
        return appendResult("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.EXECUTION_PERMITTED, envelope.getVin(), envelope, result);
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        ExecutionCreateCmd cmd = treeToValue(payload, ExecutionCreateCmd.class);
        return appendRejected("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.EXECUTION_PERMIT_DENIED, envelope.getVin(), envelope,
                "OTA-PERMIT-CONFLICT", reason);
    }
}
