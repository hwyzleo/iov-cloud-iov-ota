package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionEventCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionEventResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionEventAppService;
import org.springframework.stereotype.Component;
import vehicle.fota.v1.Execution.EventResponse;
import vehicle.fota.v1.Execution.ExecutionEvent;
import vehicle.fota.v1.Types.SequenceRange;

import java.util.List;

/**
 * 安装事件命令处理器（CR-014 §5：vehicle.fota.v1.ExecutionEvent，EVENT 形态）
 *
 * <p>顺序事件、连续水位与缺失范围（US-080）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class ExecutionEventCommandHandler {

    private final ExecutionEventAppService executionEventAppService;

    public EventResponse handle(FotaMessageMetadata md, ExecutionEvent req) {
        ExecutionEventCmd cmd = new ExecutionEventCmd();
        cmd.setVin(md.vin());
        cmd.setExecutionId(parseLong(md.executionId()));
        cmd.setEventId(req.getEventId());
        cmd.setSequenceNo(req.getSequenceNo());
        cmd.setEventType(req.getStage() == null ? "INSTALL" : req.getStage());
        cmd.setEventDigest(req.hasEventDigest() ? req.getEventDigest().getValueHex() : null);
        cmd.setEventPayload(FotaJson.toJson(req));

        ExecutionEventResult result = executionEventAppService.receiveEvent(cmd);

        EventResponse.Builder b = EventResponse.newBuilder()
                .setStatus(FotaProtocols.ok())
                .setEventDisposition(FotaProtocols.eventDisposition(result.getDisposition()))
                .setAcceptedSequenceNo(result.getAcceptedSequenceNo());
        if (result.getMissingSequenceRanges() != null) {
            for (long[] r : result.getMissingSequenceRanges()) {
                b.addMissingSequenceRanges(SequenceRange.newBuilder()
                        .setStart(r[0]).setEnd(r[1]).build());
            }
        }
        return b.build();
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
