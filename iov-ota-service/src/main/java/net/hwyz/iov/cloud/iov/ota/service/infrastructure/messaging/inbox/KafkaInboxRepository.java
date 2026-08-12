package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.KafkaInboxMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka 上行消息 Inbox 仓库（CR-013 §5/§6）
 *
 * <p>以 UK(consumer_name, business_key) + payload_digest 实现消息级幂等：
 * 同业务键同摘要 → 幂等（已处理）；同业务键异摘要 → 冲突（由调用方生产业务拒绝事件）。
 * selectForUpdate 需在业务事务内调用，利用唯一索引间隙锁串行化同键并发处理。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class KafkaInboxRepository {

    private final KafkaInboxMapper kafkaInboxMapper;

    /**
     * 在业务事务内锁定已处理的同键记录；不存在则返回 null。
     *
     * @param consumerName 消费者/消息类型
     * @param businessKey  业务唯一键
     * @return 已存在记录；无则 null
     */
    @Transactional
    public KafkaInboxPo selectForUpdate(String consumerName, String businessKey) {
        return kafkaInboxMapper.selectForUpdate(consumerName, businessKey);
    }

    /**
     * 业务处理成功后写入 Inbox 处理结果索引。
     */
    @Transactional
    public void markProcessed(KafkaInboxPo po) {
        kafkaInboxMapper.insert(po);
    }

    /**
     * 记录冲突/失败等结果。
     */
    @Transactional
    public void markResult(String consumerName, String businessKey, String status,
                           Long resultMessageId, String errorReason) {
        kafkaInboxMapper.updateProcessResult(consumerName, businessKey, status, resultMessageId, errorReason);
    }
}
