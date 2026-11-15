package net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.ManifestDigestMetrics;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 包清单摘要校验指标实现（CR-024 §4.28.6）。
 *
 * <p>沿用本仓既有指标风格（{@link KafkaMessagingMetricsService}）：进程内 AtomicLong 计数，
 * 可后续接入 Micrometer/Prometheus 导出。两类事件独立计数，禁止合并。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class ManifestDigestMetricsService implements ManifestDigestMetrics {

    /** 云端未冻结摘要而跳过校验 */
    public static final String UNVERIFIABLE = "ota.permit.manifest.digest.unverifiable";

    /** 车端摘要与云端重算值不一致 */
    public static final String MISMATCH = "ota.permit.manifest.digest.mismatch";

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public void unverifiable() {
        increment(UNVERIFIABLE);
    }

    @Override
    public void mismatch() {
        increment(MISMATCH);
    }

    /**
     * 读取指定指标当前计数。
     *
     * @param metric 指标名
     * @return 计数
     */
    public long count(String metric) {
        AtomicLong counter = counters.get(metric);
        return counter == null ? 0L : counter.get();
    }

    private void increment(String metric) {
        counters.computeIfAbsent(metric, k -> new AtomicLong()).incrementAndGet();
    }
}
