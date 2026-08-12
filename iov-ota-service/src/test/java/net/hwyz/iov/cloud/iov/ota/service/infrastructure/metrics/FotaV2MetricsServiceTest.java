package net.hwyz.iov.cloud.iov.ota.service.infrastructure.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FotaV2MetricsService 监控指标测试（CR-012 §10）
 *
 * @author hwyz_leo
 */
@DisplayName("FotaV2MetricsService 监控指标")
class FotaV2MetricsServiceTest {

    private FotaV2MetricsService metrics;

    @BeforeEach
    void setUp() {
        metrics = new FotaV2MetricsService();
    }

    @Test
    @DisplayName("计数器递增")
    void increment_countersIncrement() {
        metrics.increment(FotaV2MetricsService.DETECT_TOTAL);
        metrics.increment(FotaV2MetricsService.DETECT_TOTAL);
        metrics.increment(FotaV2MetricsService.DETECT_FULL);

        Map<String, Long> snapshot = metrics.snapshot();
        assertEquals(2L, snapshot.get(FotaV2MetricsService.DETECT_TOTAL));
        assertEquals(1L, snapshot.get(FotaV2MetricsService.DETECT_FULL));
    }

    @Test
    @DisplayName("安装成功率计算")
    void installSuccessRate() {
        metrics.recordInstallResult(true);
        metrics.recordInstallResult(true);
        metrics.recordInstallResult(false);

        assertEquals(2.0 / 3.0, metrics.installSuccessRate(), 0.0001);
        assertEquals(3L, metrics.snapshot().get("install.total"));
        assertEquals(2L, metrics.snapshot().get("install.success"));
    }

    @Test
    @DisplayName("无记录时成功率为 1.0")
    void installSuccessRate_empty() {
        assertEquals(1.0, metrics.installSuccessRate(), 0.0001);
    }
}
