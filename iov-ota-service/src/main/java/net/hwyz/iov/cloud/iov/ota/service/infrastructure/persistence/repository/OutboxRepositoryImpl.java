package net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.OutboxRepository;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.OutboxMessageMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.OutboxMessagePo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事务性 Outbox 仓库实现（CR-012 §10）
 *
 * <p>iov-ota 本地轻量实现；接口设计为通用形态，未来可抽取到公共包。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxRepository {

    private final OutboxMessageMapper outboxMessageMapper;

    @Override
    public Long append(String aggregateType, String aggregateId, String eventType, String payloadJson) {
        OutboxMessagePo po = OutboxMessagePo.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payloadJson(payloadJson)
                .occurredAt(Date.from(Instant.now()))
                .status("PENDING")
                .retryCount(0)
                .build();
        outboxMessageMapper.insert(po);
        return po.getId();
    }

    @Override
    public List<OutboxMessage> findPending(int limit) {
        return outboxMessageMapper.selectPending(limit).stream()
                .map(po -> new OutboxMessage(
                        po.getId(), po.getAggregateType(), po.getAggregateId(),
                        po.getEventType(), po.getPayloadJson(),
                        po.getOccurredAt() != null ? po.getOccurredAt().toInstant() : null,
                        po.getStatus(), po.getRetryCount() != null ? po.getRetryCount() : 0))
                .collect(Collectors.toList());
    }

    @Override
    public void markPublished(Long messageId) {
        outboxMessageMapper.markPublished(messageId);
    }

    @Override
    public void markFailed(Long messageId, String reason) {
        outboxMessageMapper.markFailed(messageId, reason);
    }
}
