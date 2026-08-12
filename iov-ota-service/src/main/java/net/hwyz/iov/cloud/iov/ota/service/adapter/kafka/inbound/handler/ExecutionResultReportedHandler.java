package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionFinalizeCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionFinalizeResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 最终结果收口处理器（CR-013 §4：ota.execution.result.reported → ota.execution.result.acknowledged / ota.execution.result.rejected）
 *
 * <p>水位未达最终序号时 resultAccepted=false，生产 rejected 并携带缺失范围（US-081）。
 * 业务唯一键：优先 idempotencyKey，否则 vin + executionId + resultDigest。
 *
 * @author hwyz_leo
 */
@Component
public class ExecutionResultReportedHandler extends AbstractOtaKafkaMessageHandler {

    private final ExecutionAppService executionAppService;

    public ExecutionResultReportedHandler(ObjectMapper objectMapper,
                                          KafkaOutboxRepository outboxRepository,
                                          KafkaMessagingMetricsService metrics,
                                          ExecutionAppService executionAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.executionAppService = executionAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.EXECUTION_RESULT_REPORTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        ExecutionFinalizeCmd cmd = treeToValue(payload, ExecutionFinalizeCmd.class);
        if (envelope.getIdempotencyKey() != null && !envelope.getIdempotencyKey().isBlank()) {
            return envelope.getVin() + ":" + envelope.getIdempotencyKey();
        }
        return envelope.getVin() + ":" + cmd.getExecutionId() + ":" + cmd.getResultDigest();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        ExecutionFinalizeCmd cmd = treeToValue(payload, ExecutionFinalizeCmd.class);
        cmd.setVin(envelope.getVin());
        ExecutionFinalizeResult result = executionAppService.finalizeExecution(cmd);
        metrics.increment("ota.execution.finalize");
        if (result.isResultAccepted()) {
            metrics.recordInstallResult("SUCCEEDED".equals(result.getExecutionStatus()));
            return appendResult("EXECUTION", String.valueOf(cmd.getExecutionId()),
                    OtaMessageType.EXECUTION_RESULT_ACKNOWLEDGED, String.valueOf(cmd.getExecutionId()), envelope, result);
        }
        // 水位未达 → 结果被拒绝，携带缺失范围
        metrics.increment("ota.execution.result.rejected");
        return appendRejected("EXECUTION", String.valueOf(cmd.getExecutionId()),
                OtaMessageType.EXECUTION_RESULT_REJECTED, String.valueOf(cmd.getExecutionId()), envelope,
                "OTA-RESULT-GAP", "结果收口被拒绝，存在缺失序号范围");
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        ExecutionFinalizeCmd cmd = treeToValue(payload, ExecutionFinalizeCmd.class);
        return appendRejected("EXECUTION", String.valueOf(cmd.getExecutionId()),
                OtaMessageType.EXECUTION_RESULT_REJECTED, String.valueOf(cmd.getExecutionId()), envelope,
                "OTA-RESULT-CONFLICT", reason);
    }
}
