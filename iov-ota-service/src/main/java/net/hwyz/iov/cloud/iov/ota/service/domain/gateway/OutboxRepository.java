package net.hwyz.iov.cloud.iov.ota.service.domain.gateway;

import java.time.Instant;
import java.util.List;

/**
 * 事务性 Outbox 仓库端口（CR-012 §10）
 *
 * <p>关键领域事件（TaskReleased、ExecutionCreated、ExecutionFinalized 等）通过 Outbox 保证
 * 与业务状态在同一数据库事务内提交，再由 OutboxRelay 异步投递。
 *
 * <p>本期为 iov-ota 本地实现，接口设计为通用形态，未来可抽取到公共包。
 *
 * @author hwyz_leo
 */
public interface OutboxRepository {

    /**
     * 追加一条 Outbox 消息（须在业务事务内调用）。
     *
     * @param aggregateType 聚合类型，如 TASK / VEHICLE_TASK / EXECUTION
     * @param aggregateId   聚合ID
     * @param eventType     事件类型
     * @param payloadJson   事件负载 JSON
     * @return 消息ID
     */
    Long append(String aggregateType, String aggregateId, String eventType, String payloadJson);

    /**
     * 查询待投递消息（按状态 + 时间，支持分页）。
     *
     * @param limit 最大条数
     * @return 待投递消息列表
     */
    List<OutboxMessage> findPending(int limit);

    /**
     * 标记消息已投递。
     *
     * @param messageId 消息ID
     */
    void markPublished(Long messageId);

    /**
     * 标记消息投递失败。
     *
     * @param messageId 消息ID
     * @param reason    失败原因
     */
    void markFailed(Long messageId, String reason);

    /**
     * Outbox 消息。
     */
    record OutboxMessage(Long id, String aggregateType, String aggregateId, String eventType,
                         String payloadJson, Instant occurredAt, String status, int retryCount) {
    }
}
