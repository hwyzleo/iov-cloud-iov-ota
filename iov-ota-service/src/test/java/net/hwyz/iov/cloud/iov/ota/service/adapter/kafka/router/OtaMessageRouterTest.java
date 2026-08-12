package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.router;

import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler.OtaKafkaMessageHandler;
import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.schema.OtaMessageType;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CR-013 消息路由器单元测试
 *
 * @author hwyz_leo
 */
@DisplayName("OtaMessageRouter - messageType 路由")
class OtaMessageRouterTest {

    @Test
    @DisplayName("已知消息类型路由到对应处理器")
    void resolve_knownType_returnsHandler() {
        OtaKafkaMessageHandler detectHandler = mock(OtaKafkaMessageHandler.class);
        when(detectHandler.messageType()).thenReturn(OtaMessageType.TASK_DETECT_REQUESTED);

        OtaMessageRouter router = new OtaMessageRouter(List.of(detectHandler));

        OtaKafkaMessageHandler resolved = router.resolve(OtaMessageType.TASK_DETECT_REQUESTED.getValue());
        assertSame(detectHandler, resolved);
    }

    @Test
    @DisplayName("未知消息类型抛不可恢复异常")
    void resolve_unknownType_nonRecoverable() {
        OtaMessageRouter router = new OtaMessageRouter(List.of());
        OtaKafkaMessagingException e = assertThrows(OtaKafkaMessagingException.class,
                () -> router.resolve("ota.unknown.event"));
        assertFalse(e.isRecoverable());
    }
}
