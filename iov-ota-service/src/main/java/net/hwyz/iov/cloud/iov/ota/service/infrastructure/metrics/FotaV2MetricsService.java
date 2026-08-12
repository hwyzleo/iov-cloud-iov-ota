package net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CCP FOTA v2 监控指标服务（CR-012 §10）
 *
 * <p>监控指标：
 * <ul>
 *   <li>任务检测量、FULL/DIGEST 比例及摘要失败率</li>
 *   <li>下载凭证签发、续传、RESET_OFFSET 和校验失败率</li>
 *   <li>活动 Execution 冲突、许可拒绝原因、安装成功率</li>
 *   <li>事件重复、乱序、缺口长度、连续水位延迟和补发次数</li>
 *   <li>控制下发至 APPLIED/REJECTED 的耗时</li>
 *   <li>恢复查询、人工恢复和日志上传失败数量</li>
 * </ul>
 *
 * <p>使用 AtomicLong 计数器；可扩展接入 Micrometer/Prometheus 导出。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class FotaV2MetricsService {

    /** 指标计数器 */
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    /** 安装成功率（成功/总） */
    private final AtomicLong installTotal = new AtomicLong();
    private final AtomicLong installSuccess = new AtomicLong();

    // ==================== 计数方法 ====================

    /**
     * 递增指标。
     */
    public void increment(String metric) {
        counters.computeIfAbsent(metric, k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * 记录安装结果。
     */
    public void recordInstallResult(boolean success) {
        installTotal.incrementAndGet();
        if (success) {
            installSuccess.incrementAndGet();
        }
    }

    // ==================== 指标快照 ====================

    /**
     * 获取全部计数器快照。
     */
    public Map<String, Long> snapshot() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> result.put(k, v.get()));
        result.put("install.total", installTotal.get());
        result.put("install.success", installSuccess.get());
        return result;
    }

    /**
     * 获取安装成功率（0-1）。
     */
    public double installSuccessRate() {
        long total = installTotal.get();
        return total == 0 ? 1.0 : (double) installSuccess.get() / total;
    }

    // ==================== 预定义指标名 ====================

    /** 任务检测 */
    public static final String DETECT_TOTAL = "fota.detect.total";
    public static final String DETECT_FULL = "fota.detect.full";
    public static final String DETECT_DIGEST = "fota.detect.digest";
    public static final String DETECT_DIGEST_MISMATCH = "fota.detect.digest.mismatch";

    /** 下载 */
    public static final String DOWNLOAD_AUTH = "fota.download.auth";
    public static final String DOWNLOAD_RESUME = "fota.download.resume";
    public static final String DOWNLOAD_RESET_OFFSET = "fota.download.reset.offset";
    public static final String DOWNLOAD_VERIFY_FAIL = "fota.download.verify.fail";

    /** 执行 */
    public static final String EXECUTION_CONFLICT = "fota.execution.conflict";
    public static final String EXECUTION_PERMIT_REJECT = "fota.execution.permit.reject";
    public static final String EXECUTION_FINALIZE = "fota.execution.finalize";

    /** 事件 */
    public static final String EVENT_DUPLICATE = "fota.event.duplicate";
    public static final String EVENT_OUT_OF_ORDER = "fota.event.outoforder";
    public static final String EVENT_GAP = "fota.event.gap";
    public static final String EVENT_RETRANSMIT = "fota.event.retransmit";

    /** 控制 */
    public static final String CONTROL_APPLIED = "fota.control.applied";
    public static final String CONTROL_REJECTED = "fota.control.rejected";

    /** 恢复 */
    public static final String RECOVERY_QUERY = "fota.recovery.query";
    public static final String RECOVERY_MANUAL = "fota.recovery.manual";
    public static final String LOG_UPLOAD_FAIL = "fota.log.upload.fail";
}
