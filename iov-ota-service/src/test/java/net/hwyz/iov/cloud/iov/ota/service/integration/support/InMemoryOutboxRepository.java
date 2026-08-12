package net.hwyz.iov.cloud.iov.ota.service.integration.support;

import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.OutboxRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版 Outbox 仓库（集成测试用）。
 *
 * @author hwyz_leo
 */
public class InMemoryOutboxRepository implements OutboxRepository {

    private final Map<Long, OutboxMessage> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeed = new AtomicLong();

    @Override
    public Long append(String aggregateType, String aggregateId, String eventType, String payloadJson) {
        long id = idSeed.incrementAndGet();
        store.put(id, new OutboxMessage(id, aggregateType, aggregateId, eventType,
                payloadJson, Instant.now(), "PENDING", 0));
        return id;
    }

    @Override
    public List<OutboxMessage> findPending(int limit) {
        List<OutboxMessage> result = new ArrayList<>();
        for (OutboxMessage m : store.values()) {
            if ("PENDING".equals(m.status()) && result.size() < limit) {
                result.add(m);
            }
        }
        return result;
    }

    @Override
    public void markPublished(Long messageId) {
        OutboxMessage m = store.get(messageId);
        if (m != null) {
            store.put(messageId, new OutboxMessage(m.id(), m.aggregateType(), m.aggregateId(),
                    m.eventType(), m.payloadJson(), m.occurredAt(), "PUBLISHED", m.retryCount()));
        }
    }

    @Override
    public void markFailed(Long messageId, String reason) {
        OutboxMessage m = store.get(messageId);
        if (m != null) {
            store.put(messageId, new OutboxMessage(m.id(), m.aggregateType(), m.aggregateId(),
                    m.eventType(), m.payloadJson(), m.occurredAt(), "FAILED", m.retryCount() + 1));
        }
    }

    public long count() {
        return store.size();
    }

    public List<OutboxMessage> all() {
        return new ArrayList<>(store.values());
    }
}
