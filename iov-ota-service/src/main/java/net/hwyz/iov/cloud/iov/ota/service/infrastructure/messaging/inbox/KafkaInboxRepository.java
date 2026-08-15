package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.KafkaInboxMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka 上行消息 Inbox 仓库（CR-014 §4.2/§8）
 *
 * <p>以 UK(consumer_name, message_id) + envelope_sha256 实现 Envelope 级幂等：
 * 同 message_id 同摘要 → 幂等复用；同 message_id 异摘要 → 冲突隔离。
 * selectForUpdate 需在业务事务内调用，利用唯一索引串行化同键并发处理。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class KafkaInboxRepository {

    private final KafkaInboxMapper kafkaInboxMapper;

    /**
     * 在业务事务内锁定已处理的同 message_id 记录；不存在返回 null。
     */
    @Transactional
    public KafkaInboxPo selectForUpdate(String consumerName, String messageId) {
        return kafkaInboxMapper.selectForUpdate(consumerName, messageId);
    }

    /**
     * 业务处理成功后写入 Inbox 处理结果索引（与领域状态同事务）。
     */
    @Transactional
    public void markProcessed(KafkaInboxPo po) {
        kafkaInboxMapper.insert(po);
    }

    /**
     * 同 message_id 不同 Envelope 摘要冲突 → 标记隔离并留痕。
     */
    @Transactional
    public void markConflict(String consumerName, String messageId, String reason) {
        kafkaInboxMapper.markConflict(consumerName, messageId, reason);
    }
}
