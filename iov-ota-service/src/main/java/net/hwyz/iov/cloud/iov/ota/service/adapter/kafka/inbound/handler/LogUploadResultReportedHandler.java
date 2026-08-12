package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogResultCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.service.LogAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 日志上传结果处理器（CR-013 §4：ota.log.upload-result.reported → ota.log.upload-result.acknowledged）
 *
 * <p>业务唯一键：logRequestId。
 *
 * @author hwyz_leo
 */
@Component
public class LogUploadResultReportedHandler extends AbstractOtaKafkaMessageHandler {

    private final LogAppService logAppService;

    public LogUploadResultReportedHandler(ObjectMapper objectMapper,
                                          KafkaOutboxRepository outboxRepository,
                                          KafkaMessagingMetricsService metrics,
                                          LogAppService logAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.logAppService = logAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.LOG_UPLOAD_RESULT_REPORTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        LogResultCmd cmd = treeToValue(payload, LogResultCmd.class);
        return envelope.getVin() + ":" + cmd.getLogRequestId();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        LogResultCmd cmd = treeToValue(payload, LogResultCmd.class);
        cmd.setVin(envelope.getVin());
        logAppService.submitLogResult(cmd);
        metrics.increment("ota.log.result");
        return appendResult("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.LOG_UPLOAD_RESULT_ACKNOWLEDGED, envelope.getVin(), envelope,
                java.util.Map.of("logRequestId", cmd.getLogRequestId(), "uploadResult", cmd.getUploadResult(),
                        "acknowledged", true));
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        LogResultCmd cmd = treeToValue(payload, LogResultCmd.class);
        return appendRejected("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.LOG_UPLOAD_RESULT_ACKNOWLEDGED, envelope.getVin(), envelope,
                "OTA-LOG-CONFLICT", reason);
    }
}
