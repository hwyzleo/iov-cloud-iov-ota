package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DispositionResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 本地任务/缓存处置结果处理器（CR-013 §4：ota.task.disposition.reported → ota.task.disposition.acknowledged）
 *
 * <p>受理车辆对本地任务/包缓存处置意图的上报（US-076）。
 * 业务唯一键：vehicleTaskId + taskRevision + disposition。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TaskDispositionReportedHandler extends AbstractOtaKafkaMessageHandler {

    public TaskDispositionReportedHandler(ObjectMapper objectMapper,
                                          KafkaOutboxRepository outboxRepository,
                                          KafkaMessagingMetricsService metrics) {
        super(objectMapper, outboxRepository, metrics);
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.TASK_DISPOSITION_REPORTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        DispositionResultCmd cmd = treeToValue(payload, DispositionResultCmd.class);
        return envelope.getVin() + ":" + cmd.getVehicleTaskId() + ":" + cmd.getTaskRevision() + ":" + cmd.getDisposition();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        DispositionResultCmd cmd = treeToValue(payload, DispositionResultCmd.class);
        cmd.setVin(envelope.getVin());
        // TODO(CR-012 US-076): 本地任务/缓存处置结果受理 —— 更新 VehicleTask.local_disposition/package_cache_action
        log.info("车辆[{}]上报本地处置结果，车辆任务[{}]，处置[{}]，缓存动作[{}]",
                cmd.getVin(), cmd.getVehicleTaskId(), cmd.getDisposition(), cmd.getPackageCacheAction());
        metrics.increment("ota.disposition.total");
        return appendResult("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.TASK_DISPOSITION_ACKNOWLEDGED,
                envelope.getVin(), envelope,
                java.util.Map.of("vehicleTaskId", cmd.getVehicleTaskId(), "disposition", cmd.getDisposition(),
                        "acknowledged", true));
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        DispositionResultCmd cmd = treeToValue(payload, DispositionResultCmd.class);
        return appendRejected("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.TASK_DISPOSITION_ACKNOWLEDGED, envelope.getVin(), envelope,
                "OTA-DISPOSITION-CONFLICT", reason);
    }
}
