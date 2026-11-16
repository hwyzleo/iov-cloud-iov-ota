package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.DownloadProgressCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.DownloadProgressResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota.FotaProtocols;
import net.hwyz.iov.cloud.iov.ota.service.application.service.DownloadProgressAppService;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics.KafkaMessagingMetricsService;
import org.springframework.stereotype.Component;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Download.DownloadProgressReport;
import vehicle.fota.v1.Download.DownloadProgressResponse;
import vehicle.fota.v1.Download.DownloadState;

import java.time.Instant;

/**
 * 下载进度上报 handler（IOV-OTA-DSN-CR-026 §5、PAR-PROTO-DSN-CR-011）。
 *
 * <p>下载进度不进入安装 Execution 事件水位：本 handler 只写进度事实与响应，
 * 不创建 Execution、不推进 READY_TO_INSTALL。确定性拒绝以 payload 处置表达，
 * 系统级状态保持 ok，避免无限重投。</p>
 *
 * @author hwyz_leo
 */
@Component
public class DownloadProgressPayloadHandler extends AbstractFotaPayloadHandler<DownloadProgressReport> {

    private final DownloadProgressAppService downloadProgressAppService;

    public DownloadProgressPayloadHandler(DownloadProgressAppService downloadProgressAppService,
                                         FotaEnvelopeFactory envelopeFactory,
                                         FotaOutboxAppender outboxAppender,
                                         KafkaMessagingMetricsService metrics) {
        super(envelopeFactory, outboxAppender, metrics);
        this.downloadProgressAppService = downloadProgressAppService;
    }

    @Override
    public String payloadType() {
        return "vehicle.fota.v1.DownloadProgressReport";
    }

    @Override
    public MessageKind messageKind() {
        return MessageKind.MESSAGE_KIND_REQUEST;
    }

    @Override
    public DownloadProgressReport parse(ByteString payloadBytes) throws InvalidProtocolBufferException {
        return DownloadProgressReport.parseFrom(payloadBytes);
    }

    @Override
    public String businessKey(FotaMessageMetadata metadata, DownloadProgressReport payload) {
        return metadata.vin() + ":" + payload.getProgressId();
    }

    @Override
    public Long handle(FotaMessageMetadata metadata, DownloadProgressReport payload) {
        DownloadProgressResult result = downloadProgressAppService.receive(toCmd(metadata, payload));
        DownloadProgressResponse response = DownloadProgressResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setAccepted(result.isAccepted())
                .setDisposition(disposition(result.getDisposition()))
                .setLatestAcceptedProgressSeq(result.getLatestAcceptedProgressSeq() == null
                        ? 0L : result.getLatestAcceptedProgressSeq())
                .build();
        if (result.getDenyReason() != null) {
            response = response.toBuilder().setDenyReason(result.getDenyReason()).build();
        }
        return appendResponse(metadata, response, "VEHICLE_TASK",
                metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }

    @Override
    public Long handleConflict(FotaMessageMetadata metadata, DownloadProgressReport payload, String reason) {
        DownloadProgressResponse response = DownloadProgressResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setAccepted(false)
                .setDisposition(
                        vehicle.fota.v1.Download.DownloadProgressDisposition.DOWNLOAD_PROGRESS_DISPOSITION_CONFLICT)
                .setDenyReason(reason)
                .build();
        return appendResponse(metadata, response, "VEHICLE_TASK",
                metadata.vehicleTaskId() == null ? metadata.vin() : metadata.vehicleTaskId());
    }

    private static DownloadProgressCmd toCmd(FotaMessageMetadata metadata, DownloadProgressReport payload) {
        return DownloadProgressCmd.builder()
                .progressId(payload.getProgressId())
                .vehicleTaskId(parseLong(payload.getVehicleTaskId()))
                .packageId(payload.getPackageId())
                .packageRevision(payload.getPackageRevision())
                .downloadAttemptNo(payload.getDownloadAttemptNo() < 1 ? null : payload.getDownloadAttemptNo())
                .progressSeq(payload.getProgressSeq() < 1 ? null : payload.getProgressSeq())
                .downloadState(stateOf(payload.getDownloadState()))
                .downloadedBytes(payload.getDownloadedBytes())
                .totalBytes(payload.getTotalBytes())
                .resumeOffset(payload.getResumeOffset())
                .occurredAt(payload.getOccurredAtMs() > 0 ? Instant.ofEpochMilli(payload.getOccurredAtMs()) : null)
                .eventDigest(payload.hasEventDigest() ? payload.getEventDigest().getValueHex() : null)
                .errorCode(payload.hasErrorCode() ? payload.getErrorCode() : null)
                .failureReason(payload.hasFailureReason() ? payload.getFailureReason() : null)
                .etag(payload.hasEtag() ? payload.getEtag() : null)
                .vin(metadata.vin())
                .build();
    }

    private static String stateOf(DownloadState state) {
        if (state == null || state == DownloadState.DOWNLOAD_STATE_UNSPECIFIED
                || state == DownloadState.UNRECOGNIZED) {
            return null;
        }
        return state.name().replace("DOWNLOAD_STATE_", "");
    }

    private static vehicle.fota.v1.Download.DownloadProgressDisposition disposition(String value) {
        if (value == null) {
            return vehicle.fota.v1.Download.DownloadProgressDisposition.DOWNLOAD_PROGRESS_DISPOSITION_UNSPECIFIED;
        }
        return switch (value) {
            case DownloadProgressAppService.DISPOSITION_CURRENT ->
                    vehicle.fota.v1.Download.DownloadProgressDisposition.DOWNLOAD_PROGRESS_DISPOSITION_CURRENT;
            case DownloadProgressAppService.DISPOSITION_LATE ->
                    vehicle.fota.v1.Download.DownloadProgressDisposition.DOWNLOAD_PROGRESS_DISPOSITION_LATE_OBSERVATION;
            case DownloadProgressAppService.DISPOSITION_DUPLICATE ->
                    vehicle.fota.v1.Download.DownloadProgressDisposition.DOWNLOAD_PROGRESS_DISPOSITION_DUPLICATE;
            case DownloadProgressAppService.DISPOSITION_CONFLICT ->
                    vehicle.fota.v1.Download.DownloadProgressDisposition.DOWNLOAD_PROGRESS_DISPOSITION_CONFLICT;
            default ->
                    vehicle.fota.v1.Download.DownloadProgressDisposition.DOWNLOAD_PROGRESS_DISPOSITION_REJECTED;
        };
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
