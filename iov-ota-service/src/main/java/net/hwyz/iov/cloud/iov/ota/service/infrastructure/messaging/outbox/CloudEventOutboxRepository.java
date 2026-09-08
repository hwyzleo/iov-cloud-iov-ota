package net.hwyz.iov.cloud.iov.ota.service.infrastructure.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.CloudEventOutboxMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * OTA→VMD 云服务事件 Outbox 仓库（CR-019 §7）
 *
 * <p>业务事务内追加（与业务状态同事务提交），独立 Relay 轮询发布。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CloudEventOutboxRepository {

    private final CloudEventOutboxMapper cloudEventOutboxMapper;

    /**
     * 业务事务内追加一条云事件（UK(business_key) 幂等，重复返回 false）。
     *
     * @return 是否新建（false = 相同业务键已存在，复用原结果）
     */
    public boolean append(CloudEventOutboxPo po) {
        try {
            po.setPublishState(CloudEventOutboxPo.STATE_PENDING);
            po.setRetryCount(0);
            cloudEventOutboxMapper.insert(po);
            return true;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return false;
        }
    }

    public List<CloudEventOutboxPo> findPendingReady(int limit) {
        return cloudEventOutboxMapper.selectPendingReady(limit);
    }

    public boolean claim(Long messageId) {
        return cloudEventOutboxMapper.claim(messageId) > 0;
    }

    public void markPublished(Long messageId) {
        cloudEventOutboxMapper.markPublished(messageId);
    }

    public void markFailed(Long messageId, String reason, long backoffSeconds) {
        cloudEventOutboxMapper.markFailed(messageId, reason,
                Date.from(Instant.now().plusSeconds(backoffSeconds)));
    }

    public void markDead(Long messageId, String reason) {
        cloudEventOutboxMapper.markDead(messageId, reason);
    }

    public CloudEventOutboxPo findByBusinessKey(String businessKey) {
        return cloudEventOutboxMapper.selectByBusinessKey(businessKey);
    }
}
