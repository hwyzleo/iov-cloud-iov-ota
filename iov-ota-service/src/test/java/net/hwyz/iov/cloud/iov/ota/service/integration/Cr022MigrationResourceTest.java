package net.hwyz.iov.cloud.iov.ota.service.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CR-022 Flyway 迁移脚本守卫测试
 *
 * <p>本仓库测试环境无数据库（无 H2/Testcontainers），因此以脚本内容断言保证：
 * 新表 + 唯一键、历史初始化取 MAX(accepted_time)、幂等且单调不回退。
 * 真实空库完整 Flyway 链/既有库升级在目标 MySQL 环境验证。
 *
 * @author hwyz_leo
 */
@DisplayName("CR-022 迁移脚本守卫")
class Cr022MigrationResourceTest {

    private static final String MIGRATION = "db/migration/V34__cr022_vehicle_inventory_report_state.sql";

    private String readMigration() throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(MIGRATION)) {
            assertNotNull(in, "迁移脚本必须存在于 classpath：" + MIGRATION);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("新表与唯一键：每 VIN 一行运行态")
    void creates_report_state_table_with_unique_vin() throws IOException {
        String sql = readMigration();
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tb_vehicle_inventory_report_state`"));
        assertTrue(sql.contains("`vin`              VARCHAR(20)"));
        assertTrue(sql.contains("`last_reported_at` DATETIME(3)"));
        assertTrue(sql.contains("UNIQUE KEY `uk_vehicle_inventory_report_state_vin` (`vin`)"));
        assertFalse(sql.contains("`last_mode`"), "CR-022 不增加 last_mode/last_revision 等字段");
        assertFalse(sql.contains("`last_revision`"));
    }

    @Test
    @DisplayName("历史初始化：MAX(accepted_time) 且幂等单调不回退")
    void initializes_from_full_facts_idempotently() throws IOException {
        String sql = readMigration();
        assertTrue(sql.contains("MAX(accepted_time)"));
        assertTrue(sql.contains("GROUP BY vin"));
        assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
        assertTrue(sql.contains("GREATEST(`last_reported_at`, VALUES(`last_reported_at`))"));
        // 不改写历史 FULL 事实与 accepted_time
        assertFalse(sql.contains("UPDATE `tb_vehicle_inventory`"));
        assertFalse(sql.contains("ALTER TABLE `tb_vehicle_inventory`"));
    }
}
