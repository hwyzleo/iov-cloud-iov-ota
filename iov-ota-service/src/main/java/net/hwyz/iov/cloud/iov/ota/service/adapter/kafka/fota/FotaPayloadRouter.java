package net.hwyz.iov.cloud.iov.ota.service.adapter.kafka.fota;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.common.exception.OtaKafkaMessagingException;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto.ParProtoReleaseGuard;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.proto.PayloadTypeEntry;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FOTA PayloadType Router（CR-014 §5）
 *
 * <p>唯一输入是同一 PAR release 的 PayloadType registry；按 payload_type + message_kind 解析
 * handler。启动 fail-closed：INBOUND 注册类型必须有 handler，handler 必须绑定已注册 INBOUND 类型。
 * 同名类、错误 package、旧 vehicle.ota.v1 或未声明类型均 fail-closed。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FotaPayloadRouter {

    private final ParProtoReleaseGuard releaseGuard;
    private final List<FotaPayloadHandler<?>> handlers;

    private Map<String, FotaPayloadHandler<?>> handlerByType = new HashMap<>();

    @PostConstruct
    void init() {
        Map<String, FotaPayloadHandler<?>> byType = handlers.stream()
                .collect(Collectors.toMap(FotaPayloadHandler::payloadType, Function.identity(), (a, b) -> a));
        for (PayloadTypeEntry entry : releaseGuard.registry().entries().values()) {
            if (entry.isInbound() && !byType.containsKey(entry.payloadType())) {
                throw new IllegalStateException("INBOUND payload_type 缺少处理器: " + entry.payloadType());
            }
        }
        for (FotaPayloadHandler<?> handler : handlers) {
            PayloadTypeEntry entry = releaseGuard.registry().resolve(handler.payloadType());
            if (entry == null || !entry.isInbound()) {
                throw new IllegalStateException("处理器绑定非 INBOUND 注册类型: " + handler.payloadType());
            }
        }
        this.handlerByType = byType;
        log.info("FotaPayloadRouter 就绪：INBOUND 处理器 {} 个", byType.size());
    }

    /**
     * 按 payload_type 解析处理器。
     *
     * @throws OtaKafkaMessagingException 未知/未注册类型（不可恢复契约错误）
     */
    public FotaPayloadHandler<?> resolve(String payloadType) {
        FotaPayloadHandler<?> handler = handlerByType.get(payloadType);
        if (handler == null) {
            throw OtaKafkaMessagingException.nonRecoverable("payload_type 无处理器: " + payloadType);
        }
        return handler;
    }
}
