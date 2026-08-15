package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ControlAckCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionEventAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Execution.ControlAckReport;
import vehicle.fota.v1.Execution.ControlAckResponse;
import vehicle.fota.v1.Types.ControlAckStatus;

/**
 * 控制回执命令处理器（CR-014 §5：vehicle.fota.v1.ControlAckReport）
 *
 * <p>云端控制回执 RECEIVED/DEFERRED/APPLIED/REJECTED（US-080）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class ControlAckCommandHandler {

    private final ExecutionEventAppService executionEventAppService;

    public ControlAckResponse handle(FotaMessageMetadata md, ControlAckReport req) {
        if (!req.hasAck()) {
            return ControlAckResponse.newBuilder()
                    .setStatus(FotaProtocols.error("INVALID", "缺少 ack"))
                    .setAccepted(false)
                    .build();
        }
        ControlAckCmd cmd = new ControlAckCmd();
        cmd.setVin(md.vin());
        cmd.setExecutionId(parseLong(md.executionId()));
        cmd.setControlAckId(req.getAck().getControlAckId());
        cmd.setControlId(req.getAck().getControlId());
        cmd.setAckSequenceNo((int) req.getAck().getAckSequenceNo());
        cmd.setAckStatus(mapStatus(req.getAck().getStatus()));
        cmd.setAckPayload(FotaJson.toJson(req.getAck()));

        executionEventAppService.receiveControlAck(cmd);

        return ControlAckResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setAccepted(true)
                .setCurrentControlStatus(req.getAck().getStatus())
                .build();
    }

    private static String mapStatus(ControlAckStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CONTROL_ACK_STATUS_RECEIVED -> "RECEIVED";
            case CONTROL_ACK_STATUS_APPLIED -> "APPLIED";
            case CONTROL_ACK_STATUS_REJECTED -> "REJECTED";
            case CONTROL_ACK_STATUS_DEFERRED -> "DEFERRED";
            default -> null;
        };
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
