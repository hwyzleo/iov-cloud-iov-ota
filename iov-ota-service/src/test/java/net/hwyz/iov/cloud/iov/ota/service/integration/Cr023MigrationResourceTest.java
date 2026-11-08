package net.hwyz.iov.cloud.iov.ota.service.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CR-023 Flyway 迁移脚本守卫测试
 *
 * <p>测试环境无数据库，以脚本内容断言保证：
 * 静态 DDL（兼容 Druid WallFilter，禁止 PREPARE/SIGNAL/存储过程）、
 * 普通索引与唯一键分离（便于重复数据清理后只重跑唯一键）、
 * 唯一键硬失败门禁、不得自动删除/合并/改写历史行、唯一键覆盖全表（A1）。
 *
 * @author hwyz_leo
 */
@DisplayName("CR-023 迁移脚本守卫")
class Cr023MigrationResourceTest {

    private static final String INDEX_MIGRATION = "db/migration/V35__cr023_vehicle_part_index.sql";
    private static final String UNIQUE_KEY_MIGRATION = "db/migration/V36__cr023_vehicle_part_unique_key.sql";
    private static final String SCAN_SCRIPT = "docs/cr023_vehicle_part_duplicate_scan.sql";

    private String read(String resource) throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "资源必须存在于 classpath：" + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 盘点脚本位于仓库根 docs/，不在模块 classpath，兼容从模块目录或仓库根运行 */
    private String readScanScript() throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(SCAN_SCRIPT);
        if (in != null) {
            try (InputStream stream = in) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        Path fromModule = Path.of("..", SCAN_SCRIPT);
        Path path = Files.exists(fromModule) ? fromModule : Path.of(SCAN_SCRIPT);
        assertTrue(Files.exists(path), "盘点脚本必须存在：" + SCAN_SCRIPT);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("V35：仅静态 DDL 创建 (vin, device_code) 普通索引")
    void v35_creates_vin_device_index_only() throws IOException {
        String sql = read(INDEX_MIGRATION);
        assertTrue(sql.contains("ADD INDEX `idx_vehicle_part_vin_device` (`vin`, `device_code`)"));
        assertFalse(sql.contains("UNIQUE KEY"), "唯一键必须单独在 V36，避免失败后重复建普通索引");
        assertStatelessDdl(sql);
    }

    @Test
    @DisplayName("V36：唯一键单独迁移，覆盖全表 (vin, pn)（A1 策略）")
    void v36_creates_unique_key_on_vin_and_pn() throws IOException {
        String sql = read(UNIQUE_KEY_MIGRATION);
        assertTrue(sql.contains("ADD UNIQUE KEY `uk_vehicle_part_vin_pn` (`vin`, `pn`)"));
        assertFalse(sql.contains("WHERE `row_valid` = 1)"), "A1：唯一键覆盖全表，不做部分索引");
        assertStatelessDdl(sql);
    }

    @Test
    @DisplayName("Druid WallFilter 兼容：禁止 PREPARE/SIGNAL/存储过程/动态 SQL")
    void migrations_are_wall_filter_compatible() throws IOException {
        for (String sql : new String[]{read(INDEX_MIGRATION), read(UNIQUE_KEY_MIGRATION)}) {
            String executable = executableSql(sql);
            String upper = executable.toUpperCase();
            assertFalse(upper.contains("PREPARE "), "Druid WallFilter 拦截 PREPARE");
            assertFalse(upper.contains("SIGNAL SQLSTATE"), "Druid WallFilter 拦截 SIGNAL");
            assertFalse(upper.contains("CREATE PROCEDURE"), "禁止存储过程");
            assertFalse(upper.contains("DELIMITER"), "禁止 DELIMITER");
            assertFalse(upper.contains("INFORMATION_SCHEMA"), "不做动态存在性判断");
        }
    }

    @Test
    @DisplayName("唯一键门禁：重复数据由 ALTER 硬失败，注释给出盘点与 repair 指引")
    void unique_key_gate_fails_on_duplicates_with_guidance() throws IOException {
        String sql = read(UNIQUE_KEY_MIGRATION);
        assertTrue(sql.contains("docs/cr023_vehicle_part_duplicate_scan.sql"));
        assertTrue(sql.contains("flyway repair"));
        assertTrue(sql.contains("Duplicate entry"));
        assertTrue(sql.contains("迁移中止"));
        // ALTER 必须无条件执行（存在重复时由数据库失败中止，而非跳过）
        assertFalse(sql.contains("IF NOT EXISTS"));
    }

    @Test
    @DisplayName("迁移不得自动清理/改写历史事实")
    void migration_never_cleans_data() throws IOException {
        for (String sql : new String[]{executableSql(read(INDEX_MIGRATION)), executableSql(read(UNIQUE_KEY_MIGRATION))}) {
            String upper = sql.toUpperCase();
            assertFalse(upper.contains("DELETE FROM `TB_VEHICLE_PART`"));
            assertFalse(upper.contains("UPDATE `TB_VEHICLE_PART`"));
            assertFalse(upper.contains("DROP INDEX"));
            assertFalse(upper.contains("DROP KEY"));
        }
    }

    @Test
    @DisplayName("重复盘点脚本：全表 + 同 ECU 多候选 + 逻辑删除残留三类扫描")
    void duplicate_scan_script_is_complete() throws IOException {
        String sql = readScanScript();
        assertTrue(sql.contains("GROUP BY `vin`, `pn`"));
        assertTrue(sql.contains("HAVING COUNT(*) > 1"));
        assertTrue(sql.contains("GROUP BY `vin`, `device_code`"));
        assertTrue(sql.contains("`row_valid` = 0"));
        assertTrue(sql.toUpperCase().contains("不得"));
    }

    private void assertStatelessDdl(String sql) {
        // 迁移脚本只允许 DDL + 注释，不允许 SELECT 写入型语句
        assertFalse(executableSql(sql).toUpperCase().contains("INSERT INTO"));
    }

    /** 去除 -- 注释行，仅保留可执行 SQL，避免注释说明中的关键字造成误判 */
    private String executableSql(String sql) {
        return Arrays.stream(sql.split("\n"))
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
    }
}
