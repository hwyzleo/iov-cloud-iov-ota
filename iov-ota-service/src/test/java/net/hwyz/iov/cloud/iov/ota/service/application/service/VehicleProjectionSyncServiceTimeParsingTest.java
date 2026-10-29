package net.hwyz.iov.cloud.iov.ota.service.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 车辆投影必填时间解析单测（CR-021 §4.25.3）
 *
 * <p>覆盖：ISO 带 offset 绝对时刻、无时区墙上时间按显式业务时区解释、
 * blank/解析失败拒绝写入（不伪造当前时间）、JVM 默认时区漂移不变性。</p>
 */
@DisplayName("车辆投影必填时间解析（CR-021 §4.25.3）")
class VehicleProjectionSyncServiceTimeParsingTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private TimeZone previous;

    @AfterEach
    void restoreDefaultZone() {
        if (previous != null) {
            TimeZone.setDefault(previous);
        }
    }

    private void withDefaultZone(TimeZone tz) {
        previous = TimeZone.getDefault();
        TimeZone.setDefault(tz);
    }

    @Test
    @DisplayName("带 Z 的 ISO 串按绝对时刻转换，不依赖 JVM 时区")
    void offsetIsoIsAbsolute() {
        withDefaultZone(TimeZone.getTimeZone("UTC"));
        Date parsed = VehicleProjectionSyncService.parseRequiredDateTime(
                "2026-07-17T08:00:00Z", "productionTime", BUSINESS_ZONE);
        assertEquals(Instant.parse("2026-07-17T08:00:00Z"), parsed.toInstant());
    }

    @Test
    @DisplayName("带 +08:00 offset 的 ISO 串与 Z 是同一时刻")
    void offsetWithEightHoursIsSameInstant() {
        Date parsed = VehicleProjectionSyncService.parseRequiredDateTime(
                "2026-07-17T16:00:00+08:00", "productionTime", BUSINESS_ZONE);
        assertEquals(Instant.parse("2026-07-17T08:00:00Z"), parsed.toInstant());
    }

    @Test
    @DisplayName("无时区遗留格式按显式业务时区（Asia/Shanghai）解释")
    void noZoneLegacyPatternUsesBusinessZone() {
        Date parsed = VehicleProjectionSyncService.parseRequiredDateTime(
                "2026-07-17T08:00:00", "productionTime", BUSINESS_ZONE);
        // Asia/Shanghai 墙上 08:00 = UTC 00:00，不整体加减八小时之外的歧义
        assertEquals(Instant.parse("2026-07-17T00:00:00Z"), parsed.toInstant());
    }

    @Test
    @DisplayName("无时区 ISO_LOCAL（含小数秒）按显式业务时区解释")
    void noZoneIsoLocalWithFractionUsesBusinessZone() {
        Date parsed = VehicleProjectionSyncService.parseRequiredDateTime(
                "2026-07-17T08:00:00.123", "productionTime", BUSINESS_ZONE);
        assertEquals(Instant.parse("2026-07-17T00:00:00.123Z"), parsed.toInstant());
    }

    @Test
    @DisplayName("同一输入在 UTC / Asia-Shanghai JVM 下结果一致（无 systemDefault 漂移）")
    void deterministicAcrossJvmDefaultZones() {
        Date inUtc;
        Date inShanghai;
        withDefaultZone(TimeZone.getTimeZone("UTC"));
        inUtc = VehicleProjectionSyncService.parseRequiredDateTime(
                "2026-07-17T08:00:00", "productionTime", BUSINESS_ZONE);
        withDefaultZone(TimeZone.getTimeZone("Asia/Shanghai"));
        inShanghai = VehicleProjectionSyncService.parseRequiredDateTime(
                "2026-07-17T08:00:00", "productionTime", BUSINESS_ZONE);
        assertEquals(inUtc, inShanghai);
    }

    @Test
    @DisplayName("blank / null 必填缺失：拒绝写入，不伪造当前时间")
    void blankOrNullRejectsWrite() {
        assertThrows(IllegalStateException.class,
                () -> VehicleProjectionSyncService.parseRequiredDateTime("", "productionTime", BUSINESS_ZONE));
        assertThrows(IllegalStateException.class,
                () -> VehicleProjectionSyncService.parseRequiredDateTime(null, "occurredAt", BUSINESS_ZONE));
    }

    @Test
    @DisplayName("非空但解析失败：拒绝写入，不伪装成正常缺失")
    void invalidValueRejectsWrite() {
        assertThrows(IllegalStateException.class,
                () -> VehicleProjectionSyncService.parseRequiredDateTime("not-a-date", "productionTime", BUSINESS_ZONE));
    }

    @Test
    @DisplayName("错误信息携带字段名与原始值，便于隔离定位")
    void errorMessageCarriesFieldNameAndValue() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> VehicleProjectionSyncService.parseRequiredDateTime(
                        "2026-13-40T99:00:00", "produceTime", BUSINESS_ZONE));
        assertTrue(ex.getMessage().contains("produceTime"), ex.getMessage());
        assertTrue(ex.getMessage().contains("2026-13-40T99:00:00"), ex.getMessage());
    }
}
