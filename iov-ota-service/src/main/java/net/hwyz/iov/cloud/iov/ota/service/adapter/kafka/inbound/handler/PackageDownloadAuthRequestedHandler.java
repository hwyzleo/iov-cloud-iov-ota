package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaKafkaEnvelope;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DownloadAuthCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DownloadAuthResult;
import net.hwyz.iov.cloud.iov.ota.service.application.service.PackageDeliveryAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox.KafkaOutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;

/**
 * 下载授权处理器（CR-013 §4：ota.package.download-authorization.requested → ota.package.download-authorized / ota.package.download-denied）
 *
 * <p>业务唯一键：vin + vehicleTaskId + packageId + operation + offset（同一次凭证申请幂等）。
 *
 * @author hwyz_leo
 */
@Component
public class PackageDownloadAuthRequestedHandler extends AbstractOtaKafkaMessageHandler {

    private final PackageDeliveryAppService packageDeliveryAppService;

    public PackageDownloadAuthRequestedHandler(ObjectMapper objectMapper,
                                               KafkaOutboxRepository outboxRepository,
                                               KafkaMessagingMetricsService metrics,
                                               PackageDeliveryAppService packageDeliveryAppService) {
        super(objectMapper, outboxRepository, metrics);
        this.packageDeliveryAppService = packageDeliveryAppService;
    }

    @Override
    public OtaMessageType messageType() {
        return OtaMessageType.PACKAGE_DOWNLOAD_AUTH_REQUESTED;
    }

    @Override
    public String businessKey(OtaKafkaEnvelope envelope, JsonNode payload) {
        DownloadAuthCmd cmd = treeToValue(payload, DownloadAuthCmd.class);
        return envelope.getVin() + ":" + cmd.getVehicleTaskId() + ":" + cmd.getPackageId()
                + ":" + cmd.getOperation() + ":" + cmd.getOffset();
    }

    @Override
    public Long handle(OtaKafkaEnvelope envelope, JsonNode payload) {
        DownloadAuthCmd cmd = treeToValue(payload, DownloadAuthCmd.class);
        cmd.setVin(envelope.getVin());
        DownloadAuthResult result = packageDeliveryAppService.authorizeDownload(cmd);
        metrics.increment("ota.download.auth");
        if (result.isResetOffset()) {
            metrics.increment("ota.download.reset.offset");
        }
        return appendResult("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.PACKAGE_DOWNLOAD_AUTHORIZED, envelope.getVin(), envelope, result);
    }

    @Override
    public Long handleConflict(OtaKafkaEnvelope envelope, JsonNode payload, String reason) {
        DownloadAuthCmd cmd = treeToValue(payload, DownloadAuthCmd.class);
        return appendRejected("VEHICLE_TASK", String.valueOf(cmd.getVehicleTaskId()),
                OtaMessageType.PACKAGE_DOWNLOAD_DENIED, envelope.getVin(), envelope,
                "OTA-DOWNLOAD-CONFLICT", reason);
    }
}
