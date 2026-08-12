package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * v1 兼容事件服务（CR-012 §9.3、US-073）
 *
 * <p>v1 reportTaskProcess/reportTaskState 转换为兼容事件：使用服务端生成的 eventId/sequenceNo，
 * 仅用于旧任务，禁止与同一 Execution 的 v2 事件混用。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class CompatibilityEventService {

    /** 每任务的序号计数器（服务端生成 sequenceNo） */
    private final Map<Long, AtomicLong> sequenceCounters = new ConcurrentHashMap<>();

    /**
     * 生成服务端兼容事件。
     *
     * @param taskId    任务ID（旧任务，无 Execution）
     * @param vin       车架号
     * @param eventType 事件类型
     * @param payload   负载
     * @return 兼容事件（eventId + sequenceNo）
     */
    public CompatibleEvent composeEvent(Long taskId, String vin, String eventType, String payload) {
        long sequenceNo = sequenceCounters
                .computeIfAbsent(taskId, k -> new AtomicLong(0))
                .incrementAndGet();
        String eventId = "v1-" + UUID.randomUUID().toString().replace("-", "");
        log.info("v1兼容事件生成：task[{}] vin[{}] type[{}] eventId[{}] seq[{}]",
                taskId, vin, eventType, eventId, sequenceNo);
        return new CompatibleEvent(taskId, vin, eventId, sequenceNo, eventType, payload, Instant.now());
    }

    /**
     * v1 兼容事件。
     */
    public record CompatibleEvent(Long taskId, String vin, String eventId, long sequenceNo,
                                  String eventType, String payload, Instant occurredAt) {
    }
}
