package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.StageResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.service.PackageDeliveryAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 包阶段结果处理器（CR-013 §4：ota.package.stage-result.reported → ota.package.stage-result.acknowledged）
 *
 * <p>下载/验签/解密终态幂等落库（stageResultId）。
 * 业务唯一键：stageResultId。
 *
 * @author hwyz_leo
 */
@Component
public class PackageStageResultReportedHandler extends AbstractOtaKafkaMessageHandler {

    private final PackageDeliveryAppService packageDeliveryAppService;

    public PackageStageResultReportedHandler(ObjectMapper objectMapper,
                                             KafkaOutboxRepository outboxRepository,
                                             KafkaMessagingMetricsService metrics,
                                             PackageDeliveryAppService packageDeliveryAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.packageDeliveryAppService = packageDeliveryAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.PACKAGE_STAGE_RESULT_REPORTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        StageResultCmd cmd = treeToValue(payload, StageResultCmd.class);
        return envelope.getVin() + ":" + cmd.getStageResultId();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        StageResultCmd cmd = treeToValue(payload, StageResultCmd.class);
        cmd.setVin(envelope.getVin());
        packageDeliveryAppService.submitStageResult(cmd);
        metrics.increment("ota.package.stage.result");
        return appendResult("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.PACKAGE_STAGE_RESULT_ACKNOWLEDGED, envelope.getVin(), envelope,
                java.util.Map.of("stageResultId", cmd.getStageResultId(), "resultStatus", cmd.getResultStatus(),
                        "acknowledged", true));
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        StageResultCmd cmd = treeToValue(payload, StageResultCmd.class);
        return appendRejected("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.PACKAGE_STAGE_RESULT_ACKNOWLEDGED, envelope.getVin(), envelope,
                "OTA-STAGE-CONFLICT", reason);
    }
}
