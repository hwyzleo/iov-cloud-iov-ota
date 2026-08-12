package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DetectionCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DetectionResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.TaskDetectionAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 任务检测处理器（CR-013 §4：ota.task.detect.requested → ota.task.detected / ota.task.detect.rejected）
 *
 * <p>清单握手、任务匹配与本地任务对账。
 * 业务唯一键：VIN + 清单模式 + 清单版本 + 清单摘要（同一次握手幂等）。
 *
 * @author hwyz_leo
 */
@Component
public class TaskDetectRequestedHandler extends AbstractOtaKafkaMessageHandler {

    private final TaskDetectionAppService taskDetectionAppService;

    public TaskDetectRequestedHandler(ObjectMapper objectMapper,
                                      KafkaOutboxRepository outboxRepository,
                                      KafkaMessagingMetricsService metrics,
                                      TaskDetectionAppService taskDetectionAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.taskDetectionAppService = taskDetectionAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.TASK_DETECT_REQUESTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        return envelope.getVin() + ":"
                + text(payload, "inventoryMode") + ":"
                + value(payload, "inventoryRevision") + ":"
                + text(payload, "inventoryDigest");
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        DetectionCmd cmd = treeToValue(payload, DetectionCmd.class);
        cmd.setVin(envelope.getVin());
        DetectionResult result = taskDetectionAppService.detect(cmd);
        metrics.increment("ota.detect.total");
        if ("ACCEPTED".equals(result.getInventoryDisposition())) {
            metrics.increment("ota.detect.accepted");
        }
        return appendResult("VEHICLE", envelope.getVin(), OtaMessageType.TASK_DETECTED,
                envelope.getVin(), envelope, result);
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        return appendRejected("VEHICLE", envelope.getVin(), OtaMessageType.TASK_DETECT_REJECTED,
                envelope.getVin(), envelope, "OTA-DETECT-CONFLICT", reason);
    }

    private static String text(JsonNode payload, String field) {
        JsonNode node = payload == null ? null : payload.get(field);
        return node != null && !node.isNull() ? node.asText() : "";
    }

    private static String value(JsonNode payload, String field) {
        JsonNode node = payload == null ? null : payload.get(field);
        return node != null && !node.isNull() ? String.valueOf(node.asLong()) : "";
    }
}
