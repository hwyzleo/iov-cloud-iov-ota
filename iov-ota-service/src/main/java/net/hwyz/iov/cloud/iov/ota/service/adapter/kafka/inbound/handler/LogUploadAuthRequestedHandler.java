package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.LogAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.LogAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.LogAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 日志上传凭证申请处理器（CR-013 §4：ota.log.upload-authorization.requested → ota.log.upload-authorized）
 *
 * <p>业务唯一键：vin + vehicleTaskId + logScope。
 *
 * @author hwyz_leo
 */
@Component
public class LogUploadAuthRequestedHandler extends AbstractOtaKafkaMessageHandler {

    private final LogAppService logAppService;

    public LogUploadAuthRequestedHandler(ObjectMapper objectMapper,
                                         KafkaOutboxRepository outboxRepository,
                                         KafkaMessagingMetricsService metrics,
                                         LogAppService logAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.logAppService = logAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.LOG_UPLOAD_AUTH_REQUESTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        LogAuthCmd cmd = treeToValue(payload, LogAuthCmd.class);
        return envelope.getVin() + ":" + cmd.getVehicleTaskId() + ":" + cmd.getLogScope();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        LogAuthCmd cmd = treeToValue(payload, LogAuthCmd.class);
        cmd.setVin(envelope.getVin());
        LogAuthResult result = logAppService.authorizeLog(cmd);
        metrics.increment("ota.log.auth");
        return appendResult("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.LOG_UPLOAD_AUTHORIZED, envelope.getVin(), envelope, result);
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        LogAuthCmd cmd = treeToValue(payload, LogAuthCmd.class);
        return appendRejected("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.LOG_UPLOAD_AUTHORIZED, envelope.getVin(), envelope,
                "OTA-LOG-CONFLICT", reason);
    }
}
