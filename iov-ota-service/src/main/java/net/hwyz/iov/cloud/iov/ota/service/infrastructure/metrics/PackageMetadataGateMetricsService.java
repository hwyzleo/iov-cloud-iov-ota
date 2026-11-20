package net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ota.service.domain.gateway.PackageMetadataGateMetrics;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 包元数据门禁豁免指标实现（IOV-OTA-DSN-CR-027 §4.32.5）。
 *
 * <p>沿用本仓既有指标风格（{@code ManifestDigestMetricsService}）：进程内 AtomicLong 计数，
 * 可后续接入 Micrometer/Prometheus 导出。总量按原因码分类，标签基数受原因码闭集约束。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class PackageMetadataGateMetricsService implements PackageMetadataGateMetrics {

    /** 下发期包元数据门禁被豁免的次数 */
    public static final String BYPASSED = "ota.task-check.package.metadata.gate.bypassed";

    /** 原因码前缀（按原因码分类计数） */
    public static final String BYPASSED_REASON_PREFIX = BYPASSED + ".reason.";

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public void bypassed(String reasonCodes) {
        increment(BYPASSED);
        if (reasonCodes == null || reasonCodes.isBlank()) {
            return;
        }
        // 形状：packageId:CODE1,CODE2;packageId2:CODE3
        for (String perPackage : reasonCodes.split(";")) {
            int colon = perPackage.indexOf(':');
            String codes = colon >= 0 ? perPackage.substring(colon + 1) : perPackage;
            for (String code : codes.split(",")) {
                String trimmed = code.trim();
                if (!trimmed.isEmpty()) {
                    increment(BYPASSED_REASON_PREFIX + trimmed);
                }
            }
        }
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
