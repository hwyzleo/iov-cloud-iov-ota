package net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OTA Kafka 消息化监控指标服务（CR-013 §7）
 *
 * <p>监控指标：
 * <ul>
 *   <li>Inbox：重复率、摘要冲突率、处理失败率、消费延迟和分区积压</li>
 *   <li>Outbox：待发布量、重试次数、生产延迟和死信量</li>
 *   <li>业务：请求到结果时延、Execution 序号缺口、连续水位、控制回执和恢复失败</li>
 * </ul>
 *
 * <p>使用 AtomicLong 计数器；可扩展接入 Micrometer/Prometheus 导出。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class KafkaMessagingMetricsService {

    /** 指标计数器 */
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    /** 安装成功率（成功/总） */
    private final AtomicLong installTotal = new AtomicLong();
    private final AtomicLong installSuccess = new AtomicLong();

    /**
     * 记录安装结果。
     */
    public void recordInstallResult(boolean success) {
        installTotal.incrementAndGet();
        if (success) {
            installSuccess.incrementAndGet();
        }
    }

    /**
     * 获取安装成功率（0-1）。
     */
    public double installSuccessRate() {
        long total = installTotal.get();
        return total == 0 ? 1.0 : (double) installSuccess.get() / total;
    }

    /**
     * 递增指标。
     */
    public void increment(String metric) {
        counters.computeIfAbsent(metric, k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * 递增指定数值。
     */
    public void add(String metric, long delta) {
        counters.computeIfAbsent(metric, k -> new AtomicLong()).addAndGet(delta);
    }

    /**
     * 获取全部计数器快照。
     */
    public Map<String, Long> snapshot() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    // ==================== 预定义指标名 ====================

    /** Inbox */
    public static final String INBOX_TOTAL = "kafka.inbox.total";
    public static final String INBOX_DUPLICATE = "kafka.inbox.duplicate";
    public static final String INBOX_DIGEST_CONFLICT = "kafka.inbox.digest.conflict";
    public static final String INBOX_FAILED = "kafka.inbox.failed";
    public static final String INBOX_DLQ = "kafka.inbox.dlq";
    public static final String INBOX_BACKLOG = "kafka.inbox.backlog";

    /** Outbox */
    public static final String OUTBOX_TOTAL = "kafka.outbox.total";
    public static final String OUTBOX_PENDING = "kafka.outbox.pending";
    public static final String OUTBOX_PUBLISHED = "kafka.outbox.published";
    public static final String OUTBOX_RETRY = "kafka.outbox.retry";
    public static final String OUTBOX_DEAD = "kafka.outbox.dead";

    /** 业务 */
    public static final String EXECUTION_EVENT_GAP = "kafka.biz.event.gap";
    public static final String EXECUTION_CONTROL_ACK = "kafka.biz.control.ack";
    public static final String RECOVERY_FAILED = "kafka.biz.recovery.failed";
}
