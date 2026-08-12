package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.router;

import net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.inbound.handler.OtaKafkaMessageHandler;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OTA 消息路由器（CR-013 §1/§2）
 *
 * <p>按 messageType 将上行消息路由到对应业务处理器。
 * 未知消息类型视为不可恢复契约错误。
 *
 * @author hwyz_leo
 */
@Component
public class OtaMessageRouter {

    private final Map<String, OtaKafkaMessageHandler> handlerByType;

    public OtaMessageRouter(List<OtaKafkaMessageHandler> handlers) {
        this.handlerByType = handlers.stream()
                .collect(Collectors.toMap(
                        h -> h.messageType().getValue(),
                        Function.identity(),
                        (a, b) -> a));
    }

    /**
     * 按 messageType 解析处理器。
     *
     * @throws OtaKafkaMessagingException 未知消息类型（不可恢复）
     */
    public OtaKafkaMessageHandler resolve(String messageType) {
        OtaKafkaMessageHandler handler = handlerByType.get(messageType);
        if (handler == null) {
            throw OtaKafkaMessagingException.nonRecoverable("未知消息类型: " + messageType);
        }
        return handler;
    }
}
