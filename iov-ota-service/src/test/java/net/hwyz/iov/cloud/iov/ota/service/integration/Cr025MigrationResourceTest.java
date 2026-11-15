package net.hwyz.iov.cloud.iov.ota.service.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CR-025 Flyway 迁移脚本守卫（IOV-OTA-DSN-CR-025 §4.29.4）。
 *
 * <p>测试环境无数据库，以脚本内容断言保证：
 * 仅静态 DDL（Druid WallFilter 兼容）、审计表不可变追加、当前性索引不引入唯一约束、
 * 不做数据清洗与历史迁移改写。</p>
 *
 * @author hwyz_leo
 */
@DisplayName("CR-025 迁移脚本守卫")
class Cr025MigrationResourceTest {

    private static final String MIGRATION = "db/migration/V39__cr025_vehicle_task_reset.sql";

    private String read() throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(MIGRATION)) {
            assertNotNull(in, "资源必须存在于 classpath：" + MIGRATION);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("V39：新增车辆任务重置审计表与当前性索引")
    void v39_creates_reset_log_and_current_index() throws IOException {
        String sql = read();
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tb_task_vehicle_reset_log`"),
                "必须新增重置审计表");
        assertTrue(sql.contains("ADD INDEX `idx_task_vehicle_current` (`task_id`, `vin`, `superseded_by`)"),
                "必须新增当前车辆任务查询索引");
        // 审计必备列
        for (String column : new String[]{"old_vehicle_task_id", "new_vehicle_task_id", "old_status",
                "old_execution_id", "old_execution_status", "force_flag", "reason", "result", "create_by"}) {
            assertTrue(sql.contains("`" + column + "`"), "审计表缺少列：" + column);
        }
    }

    @Test
    @DisplayName("V39：不建立业务唯一索引（当前性由 superseded_by 表达，历史行必须保留）")
    void v39_no_unique_key_on_task_and_vin() throws IOException {
        String sql = executableSql(read());
        assertFalse(sql.toUpperCase().contains("UNIQUE KEY"),
                "不得建立 (task_id, vin) 唯一索引：历史 SUPERSEDED 行必须保留");
        assertFalse(sql.toUpperCase().contains("UNIQUE INDEX"), "不得建立唯一索引");
    }

    @Test
    @DisplayName("V39：不做数据清洗、不回填、不改写历史事实")
    void v39_never_cleans_or_backfills() throws IOException {
        String upper = executableSql(read()).toUpperCase();
        assertFalse(upper.contains("DELETE FROM"), "不得删除历史数据");
        assertFalse(upper.contains("UPDATE `TB_TASK_VEHICLE`"), "不得改写既有车辆任务行");
        assertFalse(upper.contains("DROP TABLE"), "不得删除既有表");
        assertFalse(upper.contains("DROP INDEX"), "不得删除既有索引");
        assertFalse(upper.contains("INSERT INTO"), "迁移脚本不做数据回填");
    }

    @Test
    @DisplayName("Druid WallFilter 兼容：禁止 PREPARE/SIGNAL/存储过程/动态 SQL")
    void v39_is_wall_filter_compatible() throws IOException {
        String upper = executableSql(read()).toUpperCase();
        assertFalse(upper.contains("PREPARE "), "Druid WallFilter 拦截 PREPARE");
        assertFalse(upper.contains("SIGNAL SQLSTATE"), "Druid WallFilter 拦截 SIGNAL");
        assertFalse(upper.contains("CREATE PROCEDURE"), "禁止存储过程");
        assertFalse(upper.contains("DELIMITER"), "禁止 DELIMITER");
        assertFalse(upper.contains("INFORMATION_SCHEMA"), "不做动态存在性判断");
    }

    /** 去除 -- 注释行，仅保留可执行 SQL，避免注释说明中的关键字造成误判 */
    private String executableSql(String sql) {
        return Arrays.stream(sql.split("\n"))
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
    }
}
