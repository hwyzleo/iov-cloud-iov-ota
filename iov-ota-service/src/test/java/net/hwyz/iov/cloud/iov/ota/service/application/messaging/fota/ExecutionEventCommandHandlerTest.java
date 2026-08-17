package net.hwyz.iov.cloud.iov.ota.service.application.messaging.fota;

import net.hwyz.iov.cloud.iov.ota.service.application.dto.cmd.ExecutionEventCmd;
import net.hwyz.iov.cloud.iov.ota.service.application.dto.result.ExecutionEventResult;
import net.hwyz.iov.cloud.iov.ota.service.application.messaging.FotaMessageMetadata;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ExecutionEventAppService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vehicle.common.v1.Envelope.MessageKind;
import vehicle.fota.v1.Execution.EventResponse;
import vehicle.fota.v1.Execution.ExecutionEvent;
import vehicle.fota.v1.Types.Digest;
import vehicle.fota.v1.Types.EventDisposition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 安装事件命令处理器映射单测（CR-014 §5：ExecutionEvent → ExecutionEventCmd → EventResponse）
 *
 * @author hwyz_leo
 */
@DisplayName("ExecutionEventCommandHandler - 事件 → 连续水位响应映射")
class ExecutionEventCommandHandlerTest {

    private final ExecutionEventAppService appService = mock(ExecutionEventAppService.class);
    private final ExecutionEventCommandHandler handler = new ExecutionEventCommandHandler(appService);
    private final FotaMessageMetadata md = new FotaMessageMetadata(
            "req-1", 1000L, "fota-v1", "dev-1", "LSVAU2188N2ZG4G",
            "1001", "2002", null, "vehicle.fota.v1.ExecutionEvent",
            "msg-1", null, MessageKind.MESSAGE_KIND_EVENT, null, null);

    @Test
    @DisplayName("事件字段映射 + 连续水位/缺失范围响应")
    void event_maps_and_responds_with_watermark() {
        ExecutionEvent req = ExecutionEvent.newBuilder()
                .setEventId("evt-1")
                .setSequenceNo(5L)
                .setStage("INSTALL")
                .setAttemptNo(1)
                .setEventDigest(Digest.newBuilder().setAlgorithm("sha256").setValueHex("d1").build())
                .setOccurredAtMs(1000L)
                .build();
        when(appService.receiveEvent(any())).thenReturn(ExecutionEventResult.builder()
                .disposition("ACCEPTED")
                .acceptedSequenceNo(5L)
                .missingSequenceRanges(List.of(new long[]{7L, 8L}))
                .build());

        EventResponse resp = handler.handle(md, req);

        assertEquals(EventDisposition.EVENT_DISPOSITION_ACCEPTED, resp.getEventDisposition());
        assertEquals(5L, resp.getAcceptedSequenceNo());
        assertEquals(1, resp.getMissingSequenceRangesCount());
        assertEquals(7L, resp.getMissingSequenceRanges(0).getStart());
        assertEquals(8L, resp.getMissingSequenceRanges(0).getEnd());

        ExecutionEventCmd cmd = argumentCaptor();
        assertEquals(2002L, cmd.getExecutionId());
        assertEquals("evt-1", cmd.getEventId());
        assertEquals(5L, cmd.getSequenceNo());
        assertEquals("INSTALL", cmd.getEventType());
        assertEquals("d1", cmd.getEventDigest());
        assertNotNull(cmd.getEventPayload(), "事件负载应以 JSON 承载");
    }

    private ExecutionEventCmd argumentCaptor() {
        var captor = org.mockito.ArgumentCaptor.forClass(ExecutionEventCmd.class);
        verify(appService).receiveEvent(captor.capture());
        return captor.getValue();
    }
}
