package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.KafkaOutboxMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Kafka 下行消息 Outbox 仓库（CR-014 §6/§8）
 *
 * <p>业务事务内写入领域状态与 Outbox（冻结 Envelope bytes）同事务提交；
 * 独立发布器轮询待发布行并生产已持久化 bytes。指数退避重试由发布器写入 next_retry_at。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class KafkaOutboxRepository {

    private final KafkaOutboxMapper kafkaOutboxMapper;

    /**
     * 在业务事务内追加一条下行消息（与领域状态同事务提交）。
     * Envelope bytes 必须在调用前冻结。
     *
     * @return 生成的 Outbox 消息 ID
     */
    public Long append(KafkaOutboxPo po) {
        po.setPublishState(KafkaOutboxPo.STATE_PENDING);
        po.setRetryCount(0);
        kafkaOutboxMapper.insert(po);
        return po.getId();
    }

    /**
     * 查询待发布且已到重试时间的消息。
     */
    public List<KafkaOutboxPo> findPendingReady(int limit) {
        return kafkaOutboxMapper.selectPendingReady(limit);
    }

    /**
     * 原子认领待发布消息（PENDING -> PUBLISHING），避免重复生产。
     */
    public boolean claim(Long messageId) {
        return kafkaOutboxMapper.claim(messageId) > 0;
    }

    /**
     * 标记发布成功。
     */
    public void markPublished(Long messageId) {
        kafkaOutboxMapper.markPublished(messageId);
    }

    /**
     * 标记发布失败（重试计数+1，写入下次重试时间）。
     */
    public void markFailed(Long messageId, String reason, long backoffSeconds) {
        kafkaOutboxMapper.markFailed(messageId, reason,
                Date.from(Instant.now().plusSeconds(backoffSeconds)));
    }

    /**
     * 超过最大重试次数后标记死信（人工可观测，不丢弃领域事实）。
     */
    public void markDead(Long messageId, String reason) {
        kafkaOutboxMapper.markDead(messageId, reason);
    }

    /**
     * 按传输 message_id 查询原 Outbox 消息（供投递受控重试关联）。
     */
    public KafkaOutboxPo findByMessageId(String messageId) {
        return kafkaOutboxMapper.selectByMessageId(messageId);
    }

    /**
     * 受控重试：重新入队原消息并写入下次重试时间（retry_count 递增，受上限约束）。
     *
     * @param retryAfterMs 建议重试间隔（毫秒）
     */
    public void requeue(Long id, long retryAfterMs) {
        kafkaOutboxMapper.requeue(id,
                Date.from(Instant.now().plusMillis(retryAfterMs)), "delivery-retry");
    }
}
